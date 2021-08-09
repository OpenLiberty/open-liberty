---
name: Open Liberty GA release blog post
about: Information to be included in the Open Liberty GA release blog post.
title: GA BLOG - title_of_your_update
labels: 'Blog, target:ga'
assignees: austin0, jakub-pomykala

---

The information you provide here will be included in the Open Liberty GA release blog post ([example](https://openliberty.io/blog/2020/08/05/jakarta-grpc-beta-20009.html)), which will be published on [openliberty.io/blog/](https://www.openliberty.io/blog/), and potentially elsewhere, to promote this newly released feature/function of Open Liberty. For this post to be included in the GA issue please make sure that this is completed by the end of Friday following the GM (Wednesday). 

Please provide the following information the week before the GA date (to allow for review and publishing):

1. Which Liberty feature(s) does your update relate to?
    
   Human-readable name (eg WebSockets feature):
   
   Short feature name (eg websockets-1.0): 

2. Who is the target persona? Who do you expect to use the update? eg application developer, operations.

3. Write a paragraph to summarises the update, including the following points:
   
   - A sentence or two that introduces the update to someone new to the general technology/concept.

   - What was the problem before and how does your update make their life better? (Why should they care?)
   
   - Briefly explain how to make your update work. Include screenshots, diagrams, and/or code snippets, and provide a `server.xml` snippet.
   
   - Where can they find out more about this specific update (eg Open Liberty docs, Javadoc) and/or the wider technology?

If you have previously provided this information for an Open Liberty beta blog post and nothing has changed since the beta, just provide a link to the published beta blog post and we'll take the information from there.

## What happens next?
- Add the label for the beta you're targeting: `target:YY00X-beta`.
- Make sure this blog post is linked back to the Epic for this feature/function.
- Your paragraph will be included in the GA release blog post. It might be edited for style and consistency.
- You will be asked to review a draft before publication.
- If you would _also_ like to write a standalone blog post about your update (highly recommended), raise an issue on the [Open Liberty blogs repo](https://github.com/OpenLiberty/blogs/issues/new/choose). State in the issue that the blog post relates to a specific release so that we can ensure it is published on an appropriate date (it won't be the same day as the GA blog post).
