/**
 *　　语法分析器。这是PL/0分析器中最重要的部分，在语法分析的过程中穿插着语法错误检查和目标代码生成。
 */
public class Parser {
	private Scanner lex;					// 对词法分析器的引用
	private Table table;					// 对符号表的引用
	private Interpreter interp;				// 对目标代码生成器的引用
	
	private final int symnum = Symbol.values().length;
	
	// 表示声明开始的符号集合、表示语句开始的符号集合、表示因子开始的符号集合
	// 实际上这就是声明、语句和因子的FIRST集合
	private SymSet declbegsys, statbegsys, facbegsys,exfacbegsys;
	
	/**
	 * 当前符号，由nextsym()读入
	 * @see #nextSym()
	 */
	private Symbol sym;
	
	/**
	 * 当前作用域的堆栈帧大小，或者说数据大小（data size）
	 */
	private int dx = 0;
	
	/**
	 * 构造并初始化语法分析器，这里包含了C语言版本中init()函数的一部分代码
	 * @param l 编译器的词法分析器
	 * @param t 编译器的符号表
	 * @param i 编译器的目标代码生成器
	 */
	public Parser(Scanner l, Table t, Interpreter i) {
		lex = l;
		table = t;
		interp = i;
		
		// 设置声明开始符号集
		declbegsys = new SymSet(symnum);
		declbegsys.set(Symbol.constsym);
		declbegsys.set(Symbol.varsym);
		declbegsys.set(Symbol.procsym);

		// 设置语句开始符号集
		statbegsys = new SymSet(symnum);
		statbegsys.set(Symbol.forsym);
		statbegsys.set(Symbol.beginsym);
		statbegsys.set(Symbol.callsym);
		statbegsys.set(Symbol.ifsym);
		statbegsys.set(Symbol.whilesym);
		statbegsys.set(Symbol.readsym);			// thanks to elu
		statbegsys.set(Symbol.writesym);
		statbegsys.set(Symbol.logicnot);
		statbegsys.set(Symbol.bitnot);
		statbegsys.set(Symbol.writeCh);


		// 设置因子开始符号集
		facbegsys = new SymSet(symnum);
		facbegsys.set(Symbol.ident);
		facbegsys.set(Symbol.number);
		facbegsys.set(Symbol.lparen);
		facbegsys.set(Symbol.logicnot);
		facbegsys.set(Symbol.bitnot);

		//设置扩展的因子开始符号集（++、--）
		exfacbegsys = new SymSet(symnum);
		exfacbegsys.set(Symbol.incre);
		exfacbegsys.set(Symbol.reduc);

	}
	
	/**
	 * 启动语法分析过程，此前必须先调用一次nextsym()
	 * @see #nextSym()
	 */
	public void parse() {
		SymSet nxtlev = new SymSet(symnum); //创建集合
		nxtlev.or(declbegsys); //并声明开始符号集declbegsys={29,30,31}
		nxtlev.or(statbegsys); //并语句开始符号集statbegsys={20,22,24,25,26,28}
		nxtlev.set(Symbol.period); //添加period符号（.）
		parseBlock(0, nxtlev);//nxtlev:{18,20,22,24,25,26,28,29,30,31}
		
		if (sym != Symbol.period)
			Err.report(9);
	}
	
	/**
	 * 获得下一个语法符号，这里只是简单调用一下getsym()
	 */
	public void nextSym() {
		lex.getsym();
		sym =lex.sym;
	}
	
	/**
	 * 测试当前符号是否合法
	 * 
	 * @param s1 我们需要的符号
	 * @param s2 如果不是我们需要的，则需要一个补救用的集合
	 * @param errcode 错误号
	 */
	void test(SymSet s1, SymSet s2, int errcode) {
		// 在某一部分（如一条语句，一个表达式）将要结束时时我们希望下一个符号属于某集合
		//（该部分的后跟符号），test负责这项检测，并且负责当检测不通过时的补救措施，程
		// 序在需要检测时指定当前需要的符号集合和补救用的集合（如之前未完成部分的后跟符
		// 号），以及检测不通过时的错误号。
		if(sym== Symbol.zhushi){
			return;
		}
		if (!s1.get(sym)) {
			Err.report(errcode);
			// 当检测不通过时，不停获取符号，直到它属于需要的集合或补救的集合
			while (!s1.get(sym) && !s2.get(sym))
				nextSym();
		}
	}
	
	/**
	 * 分析<分程序>
	 * 
	 * @param lev 当前分程序所在层
	 * @param fsys 当前模块后跟符号集
	 */
	public void parseBlock(int lev, SymSet fsys) {
		// <分程序> := [<常量说明部分>][<变量说明部分>][<过程说明部分>]<语句>
		
		int dx0, tx0, cx0;				// 保留初始dx，tx和cx
		SymSet nxtlev = new SymSet(symnum);
		
		dx0 = dx;						// 记录本层之前的数据量（以便恢复）
		dx = 3;
		tx0 = table.tx;					// 记录本层名字的初始位置（以便恢复）
		table.get(table.tx).adr = interp.cx;
		
		interp.gen(Fct.JMP, 0, 0);
		
		if (lev > PL0.levmax)
			Err.report(32);
		
		// 分析<说明部分>
		do {
			// <常量说明部分>
			if (sym == Symbol.constsym) {
				nextSym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do
				parseConstDeclaration(lev);
				while (sym == Symbol.comma) {
					nextSym();
					parseConstDeclaration(lev);
				}
				
				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(5);				// 漏掉了逗号或者分号
				// } while (sym == ident);
			}
			
			// <变量说明部分>
			if (sym == Symbol.varsym) {
				nextSym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do {
				parseVarDeclaration(lev);
				while (sym == Symbol.comma)
				{
					nextSym();
					parseVarDeclaration(lev);
				}
				
				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(5);				// 漏掉了逗号或者分号
				// } while (sym == ident);
			}
			
			// <过程说明部分>
			while (sym == Symbol.procsym) {
				nextSym();
				if (sym == Symbol.ident) {
					table.enter(Objekt.procedure, lev, dx);
					nextSym();
				} else { 
					Err.report(4);				// procedure后应为标识符
				}

				if (sym == Symbol.semicolon)
					nextSym();
				else
					Err.report(5);				// 漏掉了分号
				
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.semicolon);
				parseBlock(lev+1, nxtlev);
				
				if (sym == Symbol.semicolon) {
					nextSym();
					nxtlev = (SymSet) statbegsys.clone();
					nxtlev.set(Symbol.ident);
					nxtlev.set(Symbol.procsym);
					test(nxtlev, fsys, 6);
				} else { 
					Err.report(5);				// 漏掉了分号
				}
			}
			
			nxtlev = (SymSet) statbegsys.clone(); 
			nxtlev.set(Symbol.ident);
			test(nxtlev, declbegsys, 7);
		} while (declbegsys.get(sym));		// 直到没有声明符号
		
		// 开始生成当前过程代码
		Table.Item item = table.get(tx0);
		interp.code[item.adr].a = interp.cx;
		item.adr = interp.cx;					// 当前过程代码地址
		item.size = dx;							// 声明部分中每增加一条声明都会给dx增加1，
												// 声明部分已经结束，dx就是当前过程的堆栈帧大小
		cx0 = interp.cx;
		interp.gen(Fct.INT, 0, dx);			// 生成分配内存代码
		
		table.debugTable(tx0);
			
		// 分析<语句>
		nxtlev = (SymSet) fsys.clone();		// 每个后跟符号集和都包含上层后跟符号集和，以便补救
		nxtlev.set(Symbol.semicolon);		// 语句后跟符号为分号或end
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		interp.gen(Fct.OPR, 0, 0);		// 每个过程出口都要使用的释放数据段指令
		
		nxtlev = new SymSet(symnum);	// 分程序没有补救集合
		test(fsys, nxtlev, 8);				// 检测后跟符号正确性
		
		interp.listcode(cx0);
		
		dx = dx0;							// 恢复堆栈帧计数器
		table.tx = tx0;						// 回复名字表位置
	}

	/**
	 * 分析<常量说明部分>
	 * @param lev 当前所在的层次
	 */
	void parseConstDeclaration(int lev) {
		if (sym == Symbol.ident) {
			nextSym();
			if (sym == Symbol.eql || sym == Symbol.becomes) {
				if (sym == Symbol.becomes) 
					Err.report(1);			// 把 = 写成了 :=
				nextSym();
				if (sym == Symbol.number) {
					table.enter(Objekt.constant, lev, dx);
					nextSym();
				} else {
					Err.report(2);			// 常量说明 = 后应是数字
				}
			} else {
				Err.report(3);				// 常量说明标识后应是 =
			}
		} else {
			Err.report(4);					// const 后应是标识符
		}
	}

	/**
	 * 分析<变量说明部分>
	 * @param lev 当前层次
	 */
	void parseVarDeclaration(int lev) {
		if (sym == Symbol.ident) {
			// 填写名字表并改变堆栈帧计数器
			table.enter(Objekt.variable, lev, dx);
			dx ++;
			nextSym();
		} else {
			Err.report(4);					// var 后应是标识
		}
	}

	/**
	 * 分析<语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	void parseStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		// Wirth 的 PL/0 编译器使用一系列的if...else...来处理
		// 但是你的助教认为下面的写法能够更加清楚地看出这个函数的处理逻辑
		switch (sym) {
		case ident:
			parseAssignStatement(fsys, lev);
			break;
		case readsym:
			parseReadStatement(fsys, lev);
			break;
		case writesym:
			parseWriteStatement(fsys, lev);
			break;
		case callsym:
			parseCallStatement(fsys, lev);
			break;
		case ifsym:
			parseIfStatement(fsys, lev);
			break;
		case beginsym:
			parseBeginStatement(fsys, lev);
			break;
		case whilesym:
			parseWhileStatement(fsys, lev);
			break;
		case forsym:
			parseForStatement(fsys,lev);
			break;
		case logicnot:
			parseLogicnotStatement(fsys, lev);
			break;
		case bitnot:
			parseBitnotStatement(fsys, lev);
			break;
		case incre :
			parseSelfStatement(fsys, lev, Symbol.incre);
			break;
		case reduc :
			parseSelfStatement(fsys, lev, Symbol.reduc);
			break;

		case mod:
			parseModStatement(fsys,lev);
			break;
		case zhushi:
			nextSym();
			parseStatement(fsys, lev);
			break;
		case writeCh:
			parseWriteChStatement(fsys, lev);
			break;
		default:
			nxtlev = new SymSet(symnum);
			test(fsys, nxtlev, 19);
			break;
		}
	}

	private void parseModStatement(SymSet fsys, int lev) {
		nextSym();

	}

	/**
	 * 分析自增或自减运算,这个是非类似m := i++的情况。
	 * @param fsys 后跟符号表
	 * @param lev 当前层次
	 */
	private void parseSelfStatement(SymSet fsys, int lev, Symbol operator){
		int i, increOrReduc;

		if(operator == Symbol.incre) increOrReduc = 2;	//生成加法的代码
		else increOrReduc =3;		//生成减法的代码

		nextSym();
		//System.out.println("lex.id in the parseSelfStatement is:" + lex.id);
		i = table.position(lex.id);
		SymSet nxtlev;
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.rparen);
		if (i > 0) {
			Table.Item item = table.get(i);
			if (item.kind == Objekt.variable) {
				nextSym();
				interp.gen(Fct.LOD, lev-item.level, item.adr);	//取值到栈顶
				interp.gen(Fct.LIT, 0, 1);						//将1放到栈顶
				interp.gen(Fct.OPR, 0, increOrReduc);			//进行加法或减法运算
				interp.gen(Fct.STO, lev-item.level, item.adr);	//出栈取值到内存
				test(nxtlev, facbegsys, 5);
			}
			else {
				Err.report(12);	//未找到变量
			}
		}
		else {
			Err.report(11);//标识符未声明
		}
	}


	/**
			* 分析<逻辑取反语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */


	private void parseLogicnotStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		parseExpression(nxtlev, lev);
		// parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
		// interp.gen(Fct.STO, lev - item.level, item.adr);
	}

	private void parseBitnotStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		parseExpression(nxtlev, lev);
		// parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值
		// interp.gen(Fct.STO, lev - item.level, item.adr);
	}

	private void parseForStatement(SymSet fsys, int lev) {
		int cx1 , cx2 , cx3 , cx4 , cx5;
		SymSet nxtlev;
		nxtlev = (SymSet) fsys.clone();  //后跟符号集
		nextSym();
		if(sym != Symbol.lparen)  Err.report(34); //没有左括号出错
		else
		{
			nextSym();
			parseStatement(nxtlev,lev);  //第一个语句
			//语句缺少分号出错
			if(sym != Symbol.semicolon)  Err.report(10);
			else
			{
				/*cx是当前指令的地址 保存判断条件操作的位置 */
				cx1=interp.cx;  //cx:条件判断的语句位置 进入parseCondition后添加对应的判断操作（OPR 0 A）
				nextSym();
				parseCondition(nxtlev, lev);   //条件判断代码
				if(sym != Symbol.semicolon)  Err.report(10);
				else
				{
					//条件判断
					cx2=interp.cx; //记录 JPC （有条件跳转） 代码位置
					interp.gen(Fct.JPC,0,0);  //暂定地址为0
					cx3=interp.cx;  //记录JMP （无条件跳转） 代码位置 跟在JPC后，JPC没执行，就执行这个了
					interp.gen(Fct.JMP,0,0);  //暂定地址为0

					//读
					nextSym();
					cx4=interp.cx; //第三个语句分析前代码指针位置 ，即进入parseStatement后添加的第一个语句地址
					//第三个语句分析
					parseStatement(nxtlev,lev);
					if(sym != Symbol.rparen)  Err.report(22);  //缺少右括号出错
					else
					{
						interp.gen(Fct.JMP,0,cx1); // 判断条件是否满足
						nextSym();
						//保存进入for之后的第一条指令地址
						cx5=interp.cx;
						//for内容
						parseStatement(nxtlev, lev);  //代码

						//JMP跳转到cx5地址，即满足条件时，执行for中语句块的内容
						interp.code[cx3].a=cx5;

						//添加跳转cx4，即执行for括号中的第三条语句（i=i+1）
						interp.gen(Fct.JMP,0,cx4);

						//JPC(条件跳转)的位置，不满足条件时跳出循环
						interp.code[cx2].a=interp.cx;  // 反填跳出循环的地址，与if类似
					}
				}
			}
		}
	}

	/**
	 * 分析<当型循环语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseWhileStatement(SymSet fsys, int lev) {
		int cx1, cx2;
		SymSet nxtlev;
		
		cx1 = interp.cx;						// 保存判断条件操作的位置
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.dosym);				// 后跟符号为do
		parseCondition(nxtlev, lev);			// 分析<条件>
		cx2 = interp.cx;						// 保存循环体的结束的下一个位置
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转，但跳出循环的地址未知
		if (sym == Symbol.dosym)
			nextSym();
		else
			Err.report(18);						// 缺少do
		parseStatement(fsys, lev);				// 分析<语句>
		interp.gen(Fct.JMP, 0, cx1);			// 回头重新判断条件
		interp.code[cx2].a = interp.cx;			// 反填跳出循环的地址，与<条件语句>类似
	}

	/**
	 * 分析<复合语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseBeginStatement(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.semicolon);
		nxtlev.set(Symbol.endsym);
		parseStatement(nxtlev, lev);
		// 循环分析{; <语句>}，直到下一个符号不是语句开始符号或收到end
		while (statbegsys.get(sym) || sym == Symbol.semicolon) {
			if (sym == Symbol.semicolon)
				nextSym();
			else
				Err.report(10);					// 缺少分号
			parseStatement(nxtlev, lev);
		}
		if (sym == Symbol.endsym||sym == Symbol.zhushi)
			nextSym();
		else
			Err.report(17);						// 缺少end或分号
	}

	/**
	 * 分析<条件语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseIfStatement(SymSet fsys, int lev) {
		int cx1;
		SymSet nxtlev;
		
		nextSym();
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.thensym);				// 后跟符号为then或do ???
		nxtlev.set(Symbol.dosym);
		parseCondition(nxtlev, lev);			// 分析<条件>
		if (sym == Symbol.thensym)
			nextSym();
		else
			Err.report(16);						// 缺少then
		cx1 = interp.cx;						// 保存当前指令地址
		interp.gen(Fct.JPC, 0, 0);				// 生成条件跳转指令，跳转地址未知，暂时写0
		parseStatement(fsys, lev);				// 处理then后的语句
		//TODO:以下是增加的else
		/* then 语句对应的是JPC指令----->应该跳转到JMP【else】指令的后面
		 * 如果存在else语句，应该先创建else对应的JMP
		 *  */
		if(sym == Symbol.semicolon){
			nextSym();
		}
		if(sym==Symbol.elsesym){
			nextSym();
			int cx2=interp.cx;
			interp.gen(Fct.JMP, 0, 0);//经statement处理后，cx为then后语句执行
			// 完的位置，它正是前面未定的跳转地址
			interp.code[cx1].a = interp.cx;
			parseStatement(fsys, lev);//处理else之后的语句   会改变interp.cx的值
			interp.code[cx2].a = interp.cx;
		}else {
			interp.code[cx1].a = interp.cx;            // 经statement处理后，cx为then后语句执行
			// 完的位置，它正是前面未定的跳转地址
		}


	}

	/**
	 * 分析<过程调用语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseCallStatement(SymSet fsys, int lev) {
		int i;
		nextSym();
		if (sym == Symbol.ident) {
			i = table.position(lex.id);
			if (i == 0) {
				Err.report(11);					// 过程未找到
			} else {
				Table.Item item = table.get(i);
				if (item.kind == Objekt.procedure)
					interp.gen(Fct.CAL, lev - item.level, item.adr);
				else
					Err.report(15);				// call后标识符应为过程
			}
			nextSym();
		} else {
			Err.report(14);						// call后应为标识符
		}
	}

	/**
	 * 分析<写语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseWriteStatement(SymSet fsys, int lev) {
		SymSet nxtlev;

		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				nxtlev.set(Symbol.comma);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR, 0, 14);
			} while (sym == Symbol.comma);
			
			if (sym == Symbol.rparen)
				nextSym();
			else
				Err.report(33);				// write()中应为完整表达式
		}
		interp.gen(Fct.OPR, 0, 15);
	}


	/*
	*写字符
	 */
	private void parseWriteChStatement(SymSet fsys, int lev) {
		SymSet nxtlev;

		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				nxtlev.set(Symbol.comma);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR, 0, 21);
			} while (sym == Symbol.comma);

			if (sym == Symbol.rparen)
				nextSym();
			else
				Err.report(33);				// write()中应为完整表达式
		}
		interp.gen(Fct.OPR, 0, 15);
	}

	/**
	 * 分析<读语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseReadStatement(SymSet fsys, int lev) {
		int i;
		
		nextSym();
		if (sym == Symbol.lparen) {
			do {
				nextSym();
				if (sym == Symbol.ident)
					i = table.position(lex.id);
				else
					i = 0;
				
				if (i == 0) {
					Err.report(35);			// read()中应是声明过的变量名
				} else {
					Table.Item item = table.get(i);
					if (item.kind != Objekt.variable) {
						Err.report(32);		// read()中的标识符不是变量, thanks to amd
					} else {
						interp.gen(Fct.OPR, 0, 16);
						interp.gen(Fct.STO, lev-item.level, item.adr);
					}
				}
				
				nextSym();
			} while (sym == Symbol.comma);
		} else {
			Err.report(34);					// 格式错误，应是左括号
		}
		
		if (sym == Symbol.rparen) {
			nextSym();
		} else {
			Err.report(33);					// 格式错误，应是右括号
			while (!fsys.get(sym))
				nextSym();
		}
	}

	/**
	 * 分析<赋值语句>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseAssignStatement(SymSet fsys, int lev) {
		int i;
		Symbol assignInfo = Symbol.nul;	//用来保存是那种类型的赋值
		SymSet nxtlev;
		nxtlev = (SymSet) fsys.clone();

		i = table.position(lex.id);
		if (i > 0) {
			Table.Item item = table.get(i);
			if (item.kind == Objekt.variable) {
				nextSym();
				//这里nextSym()能够识别是:=还是+=这些
				if (sym == Symbol.becomes || sym == Symbol.plusEql || sym == Symbol.minusEql
						|| sym == Symbol.timesEql || sym == Symbol.slashEql){
					assignInfo = sym;
					nextSym();
				}
				else if(sym == Symbol.incre || sym == Symbol.reduc){
					//后置++--
					int increOrReduc;	//表示是++还是--
					if(sym == Symbol.incre) increOrReduc = 2;		//生成加的代码
					else increOrReduc = 3;							//生成减的代码

					nextSym();
					interp.gen(Fct.LOD, lev-item.level, item.adr);	//取值到栈顶
					interp.gen(Fct.LIT, 0, 1);						//将1放到栈顶
					interp.gen(Fct.OPR, 0, increOrReduc);			//进行加法或减法运算
					interp.gen(Fct.STO, lev-item.level, item.adr);	//出栈取值到内存
					if(sym!=Symbol.rparen) //for中右括号
						test(nxtlev, facbegsys, 5);
					return;
				} else Err.report(13);					// 没有检测到赋值符号或自增自减符号
				//:=后面一定是表达式，那+=后面呢？也是表达式！但是应该怎样处理表达式的结果？
				//是不是应该先让item入栈，然后再将parseExpression的结果入栈？
				////
				if(assignInfo != Symbol.becomes) {
					interp.gen(Fct.LOD, lev - item.level, item.adr);
				}
				////
				//这里无论是becomes还是+= -=等，都要进行表达式的分析，但是表达式中涉及不到这些的分析，会涉及到形如 m *= n++ 中n++的分析
				parseExpression(nxtlev, lev);

				switch(assignInfo) {	//增强型switch不需要break
					case plusEql -> interp.gen(Fct.OPR, lev - item.level, 2);
					case minusEql -> interp.gen(Fct.OPR, lev -item.level, 3);
					case timesEql -> interp.gen(Fct.OPR, lev - item.level, 4);
					case slashEql -> interp.gen(Fct.OPR, lev - item.level, 5);
				}
				// parseExpression将产生一系列指令，但最终结果将会保存在栈顶，执行sto命令完成赋值，都是对item的赋值
				//这里栈顶值是最后的结果，如果是b:=a++这种，应该先将a的值赋值给b,然后再自增，但是栈顶一定要是赋值给b的值，要在给b赋值完成之后再处理a吗？
				//先处理a再处理b,a先加完更新a的值，然后a的值减1作为栈顶，不更新a,这样就是b要得到的值。
				interp.gen(Fct.STO, lev - item.level, item.adr);
			} else {
				Err.report(12);						// 赋值语句格式错误
			}
		} else {
			Err.report(11);							// 变量未找到
		}
	}

	/**
	 * 分析<表达式>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseExpression(SymSet fsys, int lev) {
		Symbol addop;
		SymSet nxtlev;
		
		// 分析[+|-|!|~]<项>
		if (sym == Symbol.plus || sym == Symbol.minus|| sym == Symbol.logicnot || sym == Symbol.bitnot) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			nxtlev.set(Symbol.logicnot);
			nxtlev.set(Symbol.bitnot);
			parseTerm(nxtlev, lev);
			if (addop == Symbol.minus)
				interp.gen(Fct.OPR, 0, 1);
			else  if(addop == Symbol.logicnot){
				interp.gen(Fct.OPR, 0, 17);
			}else if(addop == Symbol.bitnot)
				interp.gen(Fct.OPR, 0, 18);
			else if(addop==Symbol.plus)
				interp.gen(Fct.OPR, 0,2);
		} else {
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			nxtlev.set(Symbol.logicnot);
			nxtlev.set(Symbol.bitnot);
			nxtlev.set(Symbol.mod);
			parseTerm(nxtlev, lev);

		}
		
		// 分析{<加法运算符><项>}
		while (sym == Symbol.plus || sym == Symbol.minus|| sym == Symbol.logicnot || sym == Symbol.bitnot) {
			addop = sym;
			nextSym();
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			nxtlev.set(Symbol.logicnot);
			nxtlev.set(Symbol.bitnot);
			parseTerm(nxtlev, lev);


			if (addop == Symbol.plus)
				interp.gen(Fct.OPR, 0, 2);
			else if(addop == Symbol.minus)
				interp.gen(Fct.OPR, 0, 3);
			else if(addop == Symbol.logicnot)
				interp.gen(Fct.OPR,0,17);
			else if(addop == Symbol.bitnot)
				interp.gen(Fct.OPR,0,18);
		}
	}

	/**
	 * 分析<项>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseTerm(SymSet fsys, int lev) {
		Symbol mulop;
		SymSet nxtlev;

		// 分析<因子>
		nxtlev = (SymSet) fsys.clone();
		nxtlev.set(Symbol.times);
		nxtlev.set(Symbol.slash);
		nxtlev.set(Symbol.logicnot);
		nxtlev.set(Symbol.bitnot);
		nxtlev.set(Symbol.mod);
		parseFactor(nxtlev, lev);



		// 分析{<乘法运算符><因子>}
		while (sym == Symbol.times || sym == Symbol.slash||sym== Symbol.mod) {
			mulop = sym;
			nextSym();
			parseFactor(nxtlev, lev);
			if (mulop == Symbol.times)
				interp.gen(Fct.OPR, 0, 4);
			else if(mulop == Symbol.slash)
				interp.gen(Fct.OPR, 0, 5);
			else{
				interp.gen(Fct.OPR,0,20);
			}
			/*else if(mulop == Symbol.logicnot){
				interp.gen(Fct.OPR,0,17);
			}else if(mulop == Symbol.bitnot)
				interp.gen(Fct.OPR,0,18);*/
		}
	}

	/**
	 * 分析<因子>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */

	private void parseFactor(SymSet fsys, int lev) {
		SymSet nxtlev;

		//这里开始分析前置++，是扩展了的因子，不是不报错
		if (exfacbegsys.get(sym)){
			parseExtendedFactor(fsys, lev);
			return;
		}
		//这里开始分析因子
		test(facbegsys, fsys, 24);			// 检测因子的开始符号
		// the original while... is problematic: var1(var2+var3)
		// thanks to macross
		// while(inset(sym, facbegsys))
		// 检测sym是不是因子开始符号,这个应该是test失败后用到的，只有在是因子的开始符号时才进行。
		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
						case constant -> interp.gen(Fct.LIT, 0, item.val);
						case variable -> interp.gen(Fct.LOD, lev - item.level, item.adr);
						case procedure -> Err.report(21);                // 不能为过程
					}
					//这里检测到是标识符，然后要看他是不是变量，如果是变量，检测后面有没有++--
					//因子和因子之间只能通过乘除运算符连接吗？
					//分析扩展的因子
					nxtlev = (SymSet) fsys.clone();
					//这里还没有改变sym,仍旧是标识符。所以加上nextSym
					nextSym();
					//如果是变量，检测到后面是扩展因子的开始符号，则进行分析。
					//变量已经入栈了。
					if (item.kind == Objekt.variable && exfacbegsys.get(sym) ) parseExtendedFactor(nxtlev, lev, i);
				} else {
					Err.report(11);					// 标识符未声明
					nextSym();
				}
			} else if (sym == Symbol.number) {	// 因子为数
				int num = lex.num;
				if (num > PL0.amax) {
					Err.report(31);
					num = 0;
				}
				interp.gen(Fct.LIT, 0, num);
				nextSym();
			} else if (sym == Symbol.lparen) {	// 因子为表达式
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				if (sym == Symbol.rparen)
					nextSym();
				else
					Err.report(22);					// 缺少右括号
			}
			else if(sym == Symbol.logicnot){
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR,0,17);
			} else if(sym == Symbol.bitnot){
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR,0,18);
			}
			else {
				test(fsys, facbegsys, 23);
			}
		}
	}

	private void parseExtendedFactor(SymSet fsys, int lev, int position) {
		SymSet nexlev;
		Table.Item item = table.get(position);
		test(exfacbegsys, fsys, 23);
		//检测sym是否在集合exfacbegsys中
		if (exfacbegsys.get(sym)) {
			switch(sym){
				case incre : {
					interp.gen(Fct.LIT, lev-item.level, 1);
					interp.gen(Fct.OPR, lev-item.level, 2);		//次栈顶与栈顶相加，结果存次栈顶,t-1
					interp.gen(Fct.STO, lev-item.level, item.adr);	//写入t-1
					interp.gen(Fct.LOD, lev-item.level, item.adr);	//重新加载到栈顶t+1
					interp.gen(Fct.LIT, 0, 1);						//1进栈
					interp.gen(Fct.OPR, 0, 3);						//次栈顶减去栈顶，结果存次栈顶t-1
					break;
				}
				case reduc : {
					interp.gen(Fct.LIT, lev-item.level, 1);
					interp.gen(Fct.OPR, lev-item.level, 3);		//次栈顶减去栈顶，结果存到次栈顶t-1
					interp.gen(Fct.STO, lev-item.level, item.adr);	//写入
					interp.gen(Fct.LOD, lev-item.level, item.adr);	//重新加载到栈顶
					interp.gen(Fct.LIT, 0, 1);						//1进栈
					interp.gen(Fct.OPR, 0, 2);						//次栈顶栈顶相加，结果存次栈顶t-1
					break;
				}
			}
			nextSym();
		}
	}
	//对前置++--的分析
	//TODO：三级项目
	/**
	 * 对前置++或--的分析
	 * @param fsys 后跟符号集
	 * @param lev 当前的层次
	 * @return int 是否是前置++--,如果是则不能用后置++--
	 */
	private void parseExtendedFactor(SymSet fsys, int lev){
		SymSet nexlev;
		int i;
		Symbol increOrReduc = Symbol.nul;
		test(exfacbegsys, fsys, 23);
		if(sym == Symbol.incre) increOrReduc = Symbol.incre;
		else increOrReduc = Symbol.reduc;
		//后置++后面要读标识符吗？在哪里读，如果读了那下一步还要分析factor
		//直接返回，不分析factor
		if(exfacbegsys.get(sym)){
			nextSym();
			if(sym == Symbol.ident) {
				i = table.position(lex.id);
				if (i > 0){
					Table.Item item = table.get(i);
					if(item.kind == Objekt.variable){
						//System.out.println("*********loading********");
						switch(increOrReduc){
							case incre : {
								//这里应该是先加加再参加其他运算。
								interp.gen(Fct.LOD, lev-item.level, item.adr);	//先取值到栈顶
								interp.gen(Fct.LIT, 0, 1);						//将1放入栈顶
								interp.gen(Fct.OPR, 0, 2);						//相加，得到要的值
								interp.gen(Fct.STO, lev-item.level, item.adr);	//写入
								interp.gen(Fct.LOD, lev-item.level, item.adr);	//重新放入栈顶
								break;
							}
							case reduc : {
								interp.gen(Fct.LOD, lev-item.level, item.adr);	//先取值到栈顶
								interp.gen(Fct.LIT, 0, 1);						//将1放入栈顶
								interp.gen(Fct.OPR, 0, 3);						//相减，得到要的值
								interp.gen(Fct.STO, lev-item.level, item.adr);	//写入
								interp.gen(Fct.LOD, lev-item.level, item.adr);	//重新放入栈顶
								break;
							}
						}
						nextSym();
					}
					else {
						Err.report(38);
					}
				}
				else {
					Err.report(11);
				}
			}
			else {
				Err.report(37);
			}
		}
	}

	/*
	private void parseFactor(SymSet fsys, int lev) {
		SymSet nxtlev;
		
		test(facbegsys, fsys, 24);			// 检测因子的开始符号
		// the original while... is problematic: var1(var2+var3)
		// thanks to macross
		// while(inset(sym, facbegsys))
		if (facbegsys.get(sym)) {
			if (sym == Symbol.ident) {			// 因子为常量或变量
				int i = table.position(lex.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
					case constant:			// 名字为常量
						interp.gen(Fct.LIT, 0, item.val);
						break;
					case variable:			// 名字为变量
						interp.gen(Fct.LOD, lev - item.level, item.adr);
						break;
					case procedure:			// 名字为过程
						Err.report(21);				// 不能为过程
						break;
					}
				} else {
					Err.report(11);					// 标识符未声明
				}
				nextSym();
			} else if (sym == Symbol.number) {	// 因子为数 
				int num = lex.num;
				if (num > PL0.amax) {
					Err.report(31);
					num = 0;
				}
				interp.gen(Fct.LIT, 0, num);
				nextSym();
			} else if (sym == Symbol.lparen) {	// 因子为表达式
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				if (sym == Symbol.rparen)
					nextSym();
				else
					Err.report(22);					// 缺少右括号
			}else if(sym == Symbol.logicnot){
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR,0,17);
			} else if(sym == Symbol.bitnot){
				nextSym();
				nxtlev = (SymSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				parseExpression(nxtlev, lev);
				interp.gen(Fct.OPR,0,18);
			}

			else {
				// 做补救措施
				test(fsys, facbegsys, 23);
			}
		}
	}*/

	/**
	 * 分析<条件>
	 * @param fsys 后跟符号集
	 * @param lev 当前层次
	 */
	private void parseCondition(SymSet fsys, int lev) {
		Symbol relop;
		SymSet nxtlev;
		
		if (sym == Symbol.oddsym) {
			// 分析 ODD<表达式>
			nextSym();
			parseExpression(fsys, lev);
			interp.gen(Fct.OPR, 0, 6);
		} else {
			// 分析<表达式><关系运算符><表达式>
			nxtlev = (SymSet) fsys.clone();
			nxtlev.set(Symbol.eql);
			nxtlev.set(Symbol.neq);
			nxtlev.set(Symbol.lss);
			nxtlev.set(Symbol.leq);
			nxtlev.set(Symbol.gtr);
			nxtlev.set(Symbol.geq);
			parseExpression(nxtlev, lev);
			if (sym == Symbol.eql || sym == Symbol.neq 
					|| sym == Symbol.lss || sym == Symbol.leq
					|| sym == Symbol.gtr || sym == Symbol.geq) {
				relop = sym;
				nextSym();
				parseExpression(fsys, lev);
				switch (relop) {
				case eql:
					interp.gen(Fct.OPR, 0, 8);
					break;
				case neq:
					interp.gen(Fct.OPR, 0, 9);
					break;
				case lss:
					interp.gen(Fct.OPR, 0, 10);
					break;
				case geq:
					interp.gen(Fct.OPR, 0, 11);
					break;
				case gtr:
					interp.gen(Fct.OPR, 0, 12);
					break;
				case leq:
					interp.gen(Fct.OPR, 0, 13);
					break;
				}
			} else {
				Err.report(20);
			}
		}
	}
}
