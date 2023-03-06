A few notes about this project.

- Try to use unique RP servers in each test class.  When we use the shrink wrapper to build the test wars at runtime, we've had collisions between server instances - If we try 
	to use the same app name in subsequent servers, we get messages that the app won't be exported as it already exists.  The app is then stopped and never fully restarts which results 
	in a 404.  But, the biggest issue is that as the classes run, we just keep ading wars to the same server...
- If we do need to re-use a server, don't re-use app names and keep in mind that we'll be adding more wars to the same server and all will be automatically started.
- Diverging from our normal SSO fat server naming so that we can have unique server names withouth them becoming too long (which has caused issues on Windows in the past)
