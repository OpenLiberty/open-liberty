---
name: Open Liberty GA release blog post
about: Information to be included in the Open Liberty GA release blog post.
title: GA BLOG - title_of_your_update
labels: 'Blog, target:ga'
assignees: ReeceNana

---

The information you provide here will be included in the Open Liberty GA release blog post ([example](https://openliberty.io/blog/2022/01/18/microprofile5-22001.html)), which will be published on [openliberty.io/blog/](https://www.openliberty.io/blog/), and potentially elsewhere, to promote this newly released feature/function of Open Liberty. For this post to be included in the GA issue please make sure that this is completed by the end of Friday following the GM (Wednesday). The beta and release blogs are created using automation and rely on you following the template's structure.  **DO NOT REMOVE/ALTER THE `<GHA>` TAGS THROUGHOUT THIS TEMPLATE.**

Please provide the following information:

1. If this was previously published in a beta blog post, then provide the link to that `OpenLiberty/open-liberty` beta blog post issue on the next line between the `<GHA-BLOG-BETA-LINK>` tags. If nothing has changed since the beta, you're done and can omit the remaining steps. If you need to make updates/alterations to the beta content, then do all the steps. 
   <GHA-BLOG-BETA-LINK>https://github.com/OpenLiberty/open-liberty/issues/0</GHA-BLOG-BETA-LINK>

   <GHA-BLOG-RELATED-FEATURES>
2. Which Liberty feature(s) does your update relate to?
    
   Human-readable name (eg WebSockets feature):
   
   Short feature name (eg websockets-1.0): 

   
   </GHA-BLOG-RELATED-FEATURES>

   <GHA-BLOG-TARGET-PERSONA>
3. Who is the target persona? Who do you expect to use the update? eg application developer, operations.
    
   
   </GHA-BLOG-TARGET-PERSONA>

   <GHA-BLOG-SUMMARY>
4. Provide a summary of the update, including the following points:
   
   - A sentence or two that introduces the update to someone new to the general technology/concept.

   - What was the problem before and how does your update make their life better? (Why should they care?)
   
   - Briefly explain how to make your update work. Include screenshots, diagrams, and/or code snippets, and provide a `server.xml` snippet.
   
   - Where can they find out more about this specific update (eg Open Liberty docs, Javadoc) and/or the wider technology?

   </GHA-BLOG-SUMMARY>

## What happens next?
- Add the label for the GA you're targeting: `target:YY00X`.
- Make sure this blog post is linked back to the Epic for this feature/function.
- Your paragraph will be included in the GA release blog post. It might be edited for style and consistency.
- You will be asked to review a draft before publication.
    - Once you've approved the code review, close this issue. 
- If you would _also_ like to write a standalone blog post about your update (highly recommended), raise an issue on the [Open Liberty blogs repo](https://github.com/OpenLiberty/blogs/issues/new/choose). State in the issue that the blog post relates to a specific release so that we can ensure it is published on an appropriate date (it won't be the same day as the GA blog post).
