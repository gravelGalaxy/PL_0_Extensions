/**
 *　各种符号的编码
 */
public enum Symbol {
	nul, 		//不能识别的符号
	ident, 		//identifier
	number, 	// number，数据类型只有整型
	plus, 		//+
	minus, 		//-
	times, 		//*
	slash,		// /
	oddsym, 	// <>
	eql, 		// =
	neq, 		// #
	lss, 		//<
	leq, 		//<=
	gtr, 		//>
	geq, 		//>=
	lparen, 	//(
	rparen,		//)
	comma, 		//,
	semicolon, 	//;
	period, 	//.
	becomes, 	// :=
	beginsym, 	//begin
	endsym, 	//end
	ifsym, 		//if
	thensym, 	//then
	whilesym, 	//while
	writesym,	//write
	readsym, 	//read
	dosym, 		//do
	callsym, 	//call
	constsym, 	//const
	varsym, 	//var
	procsym,	//procedure
	forsym,		//新增for

	logicnot,		//!

	bitnot,		//~

	incre,      //increase ++
	reduc,      //reduction --
	plusEql,	// +=
	minusEql,	// -=
	timesEql,	// *=
	slashEql,	// /=

	elsesym,     //else

	mod,//mod

	zhushi,

	writeCh
}



















