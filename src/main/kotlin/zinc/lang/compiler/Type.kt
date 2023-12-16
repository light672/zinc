package zinc.lang.compiler

open class Type(val nullable: Boolean = false) {
	object Any : Type()
	object NullableAny : Type(true)
	object Char : Type()
	object NullableChar : Type(true)

	object Byte : Type()
	object NullableByte : Type(true)

	object Short : Type()
	object NullableShort : Type(true)

	object Int : Type()
	object NullableInt : Type(true)

	object Long : Type()
	object NullableLong : Type(true)

	object Float : Type()
	object NullableFloat : Type(true)

	object Double : Type()
	object NullableDouble : Type(true)

	object String : Type()
	object NullableString : Type(true)

	data class Array(val type: Type) : Type()
	data class NullableArray(val type: Type) : Type(true)


	fun Any(nullable: Boolean) = if (nullable) Any else NullableAny
	fun Char(nullable: Boolean) = if (nullable) Char else NullableChar
	fun Byte(nullable: Boolean) = if (nullable) Byte else NullableByte
	fun Short(nullable: Boolean) = if (nullable) Short else NullableShort
	fun Int(nullable: Boolean) = if (nullable) Int else NullableInt
	fun Long(nullable: Boolean) = if (nullable) Long else NullableLong
	fun Float(nullable: Boolean) = if (nullable) Float else NullableFloat
	fun Double(nullable: Boolean) = if (nullable) Double else NullableDouble
	fun String(nullable: Boolean) = if (nullable) String else NullableString
	fun Array(type: Type, nullable: Boolean) = if (nullable) Array(type) else NullableArray(type)
}