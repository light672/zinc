import com.light672.zinc.Zinc
import java.io.File
import java.nio.charset.Charset


fun main() {
	val script = File("src/main/kotlin/script.zc").readBytes().toString(Charset.defaultCharset())
	normalTest(script)


}

fun normalTest(source: String) {
	val runtime =
		Zinc.Runtime(
			256,
			256,
			source,
			System.out,
			System.err,
		)

	runtime.run()


}

/*

use std::rand;

enum Type {
	Number(int),
	Skip,
	PlusTwo,
	Reverse
}

impl Type {
	fn get_type_from_num(num: int) -> Type {
		match num {
			0..9 => Type::Number(num),
			10 => Type::Skip,
			11 => Type::PlusTwo,
			12 => Type::Reverse,
			_ => panic("tried to make a card with a number over 12")
		}
	}
}

enum WildType {
	PlusFour,
	Swap,
	ChangeColor
}

enum Card {
	Blue(Type),
	Red(Type),
	Yellow(Type),
	Green(Type),
	Wild(WildType)
}

impl Card {
	// non-wild
	fn get_color_card_from_num(num: int, type: Type) -> Card {
		match num {
			0 => Blue(type),
			1 => Red(type),
			2 => Yellow(type),
			3 => Green(type)
		}
	}
}


struct Player {
	name: String,
	mut cards: List[Card]
}

struct Game {
	mut topCard: Card,
	deck: List[Card],
	mut deckTop: Int,

}

impl Game {
	fn new() -> Game {
		let numberList = (1..26).map(|x| x % 13);

		let cardList = List::new(
			|i| Card::get_color_card_from_num(i / 8, Type::get_type_from_num(i % 13)),
			numberList.len() * 4
		);
	}
}

fn main() {
	let numberList =
}
 */