package opendataflow

object GitHub {

  def envTokenOrThrow: String =
    sys.env.getOrElse("PR_VALIDATOR_GH_TOKEN",
      throw new Exception("No PR_VALIDATOR_GH_TOKEN env var provided, unable to reach github!"))

  def url(v: String): String = {
    val branch = if (v.endsWith("SNAPSHOT")) "master" else "v" + v
    "http://github.com/opendataflow/opendataflow/tree/" + branch
  }
}
