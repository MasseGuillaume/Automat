/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package automat

import java.lang.reflect.InvocationTargetException

import akka.http.scaladsl.model.ContentTypeRange
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import akka.util.ByteString
import org.json4s.{ Formats, MappingException, Serialization, ParserUtil }
import scala.collection.immutable.Seq

/**
  * Automatic to and from JSON marshalling/unmarshalling using an in-scope *Json4s* protocol.
  *
  */
object Json4sSupport {
  def unmarshallerContentTypes: Seq[ContentTypeRange] =
    mediaTypes.map(ContentTypeRange.apply)

  def mediaTypes: Seq[MediaType.WithFixedCharset] =
    List(`application/json`)

  private val jsonStringUnmarshaller =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset)       => data.decodeString(charset.nioCharset.name)
      }


  /**
    * HTTP entity => `A`
    *
    * @tparam A type to decode
    * @return unmarshaller for `A`
    */
  implicit def unmarshaller[A](implicit serialization: Serialization,
                                        formats: Formats,
                                        manifest: Manifest[A]): FromEntityUnmarshaller[Either[Throwable, Option[A]]] =
    jsonStringUnmarshaller.map{s =>

      def fail(cause: Throwable): Either[Throwable, Option[A]] = {
        val msg =
          s"""|Failed to Unmarshall
              |$s
              |into
              |$manifest""".stripMargin
        Left(new Exception(msg, cause))
      }

      if (s == null) Right(None)
      else {
        try (Right(Some(serialization.read(s))))
        catch {
          case ex: ParserUtil.ParseException => fail(ex)
          case MappingException(_, ite: InvocationTargetException) => fail(ite.getCause)
        }
      }

    }
}