===============================
Using the Hacker News API Documentation (https://github.com/HackerNews/API) write a program in Scala that will return, for each of the top 30 stories:

The story title
The top 10 commenters of that story. For each commenter, also return:
The number of comments they made on the story
The total number of comments they made among all the top 30 stories
The program has to parallelize requests and aggregate the results as efficiently as possible.

For example, let's consider just 3 top stories (instead of 30) and top 2 commenters (instead of 10):

| Story A            | Story B             | Story C             |
|--------------------|---------------------|---------------------|
| user-a (1 comment) | user-a (4 comments) | user-a (4 comments) |
| user-b (2 comment) | user-b (3 comments) | user-b (5 comments) |
| user-c (3 comment) | user-c (2 comments) | user-c (3 comments) |

The result would be:

| Story   | 1st Top Commenter               | 2nd Top Commenter               |
|---------|---------------------------------|---------------------------------|
| Story A | user-c (3 for story - 8 total)  | user-b (2 for story - 10 total) |
| Story B | user-a (4 for story - 9 total)  | user-b (3 for story - 10 total) |
| Story C | user-b (5 for story - 10 total) | user-a (4 for story - 9 total)  |

===============================

Focus on implementing a clean, efficient, and POLISHED solution. A little extra time spent on polishing your code is never a bad idea.

I suggest you set a time limit for yourself. 3 hours for coding. 1 hour for polish.
