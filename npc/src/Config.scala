
object Config {
    val perf_on = true
    val use_soc = sys.props.get("use_soc").exists(_.toBoolean)
    val initPC = if (use_soc) 0x30000000 else 0x80000000
}