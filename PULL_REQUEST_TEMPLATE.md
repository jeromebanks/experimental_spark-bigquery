# New PR - Checklist

## Describe your change:

[INSERT DESCRIPTION HERE]

## Checklist - Check boxes in the PR UI, not by editing the text

### Pull Request

* [ ] PR title includes a Jira Ticket ID.
* [ ] PR description is meaningful.
* [ ] There are no more than a few commits to merge.
* [ ] There are no more than a few lines of code to review.

### Code style

* [ ] Maximum line length does not require scrolling in Github
* [ ] [ScalaStyle](http://www.scalastyle.org/) installed
* [ ] [central scalaStyle](https://github.com/Demandbase/scala_code_style/blob/master/scalaStyle/scalastyle-config.xml) config used

### Testing

* [ ] Unit tests for the added/modified code 
* [ ] Overall unit test coverage good
* [ ] Jira ticket includes QA details

### Dependencies

* [ ] [sbt_safety_plugin](https://github.com/Demandbase/sbt_safety_plugin) is installed
* [ ] [sbt_safety_plugin](https://github.com/Demandbase/sbt_safety_plugin) is at the [latest version](https://github.com/Demandbase/sbt_safety_plugin/blob/master/VERSION)
* [ ] New dependencies have been vetted

### Security

* [ ] SourceClear is safe
* [ ] Veracode is safe
* [ ] Gitleaks is safe
* [ ] The PR checklist is up to date with [central PR checklist](https://github.com/Demandbase/scala_new_project/blob/master/PULL_REQUEST_TEMPLATE.md)
