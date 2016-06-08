package com.seglo.learningspark.githubstream

/**
  * Created by seglo on 2016-06-08.
  */
case class GitHubEventResponse(requestId: String, response: String, events: List[GitHubEvent])
