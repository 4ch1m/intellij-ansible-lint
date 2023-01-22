package de.achimonline.ansible_lint.parser

/**
    based on:
    - [CodeclimateJSONFormatter](https://github.com/ansible/ansible-lint/blob/fd71b3f681a4bb2f2ee458dd4aad335dc3d25f22/src/ansiblelint/formatters/__init__.py#L130)
    - [Code Climate Engine Specification](https://github.com/codeclimate/platform/blob/690633cb2a08839a5bfa350ed925ddb6de55bbdc/spec/analyzers/SPEC.md)
 */

data class AnsibleLintItem(
    val type: String = "",
    val check_name: String = "",
    val categories: List<String> = emptyList(),
    val url: String = "",
    val severity: String = "",
    val level: String = "",
    val description: String = "",
    val fingerprint: String = "",
    val location: Location = Location(),
    val content: Content = Content()
) {
    data class Location(
        val path: String = "",
        val positions: Positions = Positions(),
        val lines: BeginAndEnd = BeginAndEnd()
    ) {
        data class Positions(
            val begin: LineAndColumn = LineAndColumn(),
            val end: LineAndColumn = LineAndColumn()
        ) {
            data class LineAndColumn(
                val line: Int = Int.MIN_VALUE,
                val column: Int = Int.MIN_VALUE
            )
        }

        data class BeginAndEnd(
            val begin: Int = Int.MIN_VALUE,
            val end: Int = Int.MIN_VALUE
        )
    }

    data class Content(
        val body: String = ""
    )

    fun getLine(): Int {
        return if (location.positions.begin.line != Int.MIN_VALUE) {
            location.positions.begin.line
        } else {
            location.lines.begin
        }
    }

    fun getColumn(): Int {
        return location.positions.begin.column
    }
}
