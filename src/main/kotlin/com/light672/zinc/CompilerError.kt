package com.light672.zinc

import com.light672.zinc.ast.Token
import com.light672.zinc.ast.TokenType
import com.light672.zinc.ast.Type
import com.light672.zinc.ir.IRType
import kotlin.math.max

internal class CompilerError(
	val code: Code,
	val message: String,
	val lines: IntRange,
	val range: IntRange
) {
	companion object {
		internal fun unexpectedChar(char: Char, location: Int, line: Int) =
			CompilerError(Code.UNEXPECTED_CHAR, "Unexpected character '$char'", line..line, location..location)

		internal fun unterminatedString(char: Char, location: Int, line: Int) =
			CompilerError(Code.UNTERMINATED_STRING, "Unterminated string on line.", line..line, location..location)

		internal fun unexpectedToken(got: Token, expected: TokenType) =
			CompilerError(Code.UNEXPECTED_TOKEN, "Expected '$expected', but got '${got.lexeme}'.", got.line..got.line, got.rangeOnLine)

		internal fun expectedType(got: Token) =
			CompilerError(Code.EXPECTED_TYPE, "Expected type, got ${got.lexeme}.", got.line..got.line, got.rangeOnLine)

		internal fun expectedTypePath(got: Token) =
			CompilerError(Code.EXPECTED_TYPE_PATH, "Expected type path, got ${got.lexeme}.", got.line..got.line, got.rangeOnLine)

		internal fun expectedBlock(got: Token) =
			CompilerError(Code.EXPECTED_BLOCK, "Expected block, got ${got.lexeme}.", got.line..got.line, got.rangeOnLine)

		internal fun expectedStatement(got: Token) =
			CompilerError(Code.EXPECTED_STATEMENT, "Expected statement or expression, got ${got.lexeme}.", got.line..got.line, got.rangeOnLine)

		internal fun expectedDeclaration(got: Token) =
			CompilerError(Code.EXPECTED_DECLARATION, "Expected declaration, got ${got.lexeme}.", got.line..got.line, got.rangeOnLine)

		internal fun expectedExpression(got: Token) =
			CompilerError(Code.EXPECTED_EXPRESSION, "Expected expression, got $got.", got.line..got.line, got.rangeOnLine)

		internal fun expectedPattern(got: Token) =
			CompilerError(Code.EXPECTED_PATTERN, "Expected pattern, got $got.", got.line..got.line, got.rangeOnLine)

		internal fun nameAlreadyExists(name: Token) =
			CompilerError(Code.NAME_ALREADY_EXISTS, "'${name.lexeme}' already exists in the current scope.", name.line..name.line, name.rangeOnLine)

		internal fun nameDoesNotExist(token: Token) =
			CompilerError(
				Code.NAME_DOES_NOT_EXIST,
				"Could not find '${token.lexeme}' in the current scope.",
				token.line..token.line,
				token.rangeOnLine
			)

		internal fun nameIsNotMemberOf(name: Token, type: IRType) =
			CompilerError(
				Code.NAME_IS_NOT_MEMBER_OF,
				"${name.lexeme} is not a member of '${type}'",
				name.line..name.line,
				name.rangeOnLine
			)

		internal fun indirectTypeInImpl(type: Type) =
			CompilerError(
				Code.INDIRECT_TYPE_IN_IMPL,
				"Types in impl declaration must not use associated types or qualified paths.",
				type.first().line..type.last().line,
				type.first().rangeOnLine.first..max(type.first().rangeOnLine.last, type.last().rangeOnLine.last)
			)

		internal fun inheritingNonInterface(type: Type) =
			CompilerError(
				Code.INHERITING_NON_INTERFACE,
				"An inherent implementation must inherit an interface.",
				type.first().line..type.last().line,
				type.first().rangeOnLine.first..max(type.first().rangeOnLine.last, type.last().rangeOnLine.last)
			)

		internal fun missingInterfaceMembers(list: List<CharSequence>, implLines: IntRange, implRange: IntRange) =
			CompilerError(
				Code.MISSING_INTERFACE_MEMBERS,
				"Not all interface items implemented, missing ${list.joinToString(", ", "'", "'")}",
				implLines,
				implRange
			)
	}

	enum class Code {
		UNEXPECTED_CHAR,
		UNTERMINATED_STRING,
		UNEXPECTED_TOKEN,
		EXPECTED_TYPE,
		EXPECTED_TYPE_PATH,
		EXPECTED_BLOCK,
		EXPECTED_STATEMENT,
		EXPECTED_DECLARATION,
		EXPECTED_EXPRESSION,
		EXPECTED_PATTERN,
		NAME_ALREADY_EXISTS,
		NAME_DOES_NOT_EXIST,
		NAME_IS_NOT_MEMBER_OF,
		INDIRECT_TYPE_IN_IMPL,
		INHERITING_NON_INTERFACE,
		MISSING_INTERFACE_MEMBERS
	}
}


