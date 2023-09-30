/**
 *　　这个类只是包含了报错函数以及错误计数器�
 */
public class Err {
	/**
	 * 错误计数器，编译过程中一共有多少个错�
	 */
	public static int err = 0;
	
	/**
	 * 报错函数
	 * @param errcode 错误�
	 */
	public static void report(int errcode) {
		char[] s = new char[PL0.lex.cc-1];
		java.util.Arrays.fill(s, ' ');
		String space = new String(s);

		err ++;

			if(errcode == 1) {
				System.out.println("Error : 常数说明中的�”写成“：=� + space + "!" + errcode);
				PL0.fa1.println("Error : 常数说明中的�”写成“：=� + space + "!" + errcode);
			}
			if(errcode == 2) {
				System.out.println("Error : 常数说明中的�”后应是数字" + space + "!" + errcode);
				PL0.fa1.println("Error : 常数说明中的�”后应是数字" + space + "!" + errcode);
			}
			if(errcode == 3) {
				System.out.println("Error : 常数说明中的标识符后应是�� + space + "!" + errcode);
				PL0.fa1.println("Error : 常数说明中的标识符后应是�� + space + "!" + errcode);
			}
			if(errcode == 4) {
				System.out.println("Error : const,var,procedure后应为标识符" + space + "!" + errcode);
				PL0.fa1.println("Error : const,var,procedure后应为标识符" + space + "!" + errcode);
			}
			if(errcode == 5) {
				System.out.println("Error : 漏掉了�”或�� + space + "!" + errcode);
				PL0.fa1.println("Error : 漏掉了�”或�� + space + "!" + errcode);
			}
			if(errcode == 6) {
				System.out.println("Error : 过程说明后的符号不正�应是语句开始符,或过程定义符)" + space + "!" + errcode);
				PL0.fa1.println("Error : 过程说明后的符号不正�应是语句开始符,或过程定义符)" + space + "!" + errcode);
			}
			if(errcode == 7) {
				System.out.println("Error : 应是语句开始符" + space + "!" + errcode);
				PL0.fa1.println("Error : 应是语句开始符" + space + "!" + errcode);
			}
			if(errcode == 8) {
				System.out.println("Error : 程序体内语句部分的后跟符不正� + space + "!" + errcode);
				PL0.fa1.println("Error : 程序体内语句部分的后跟符不正� + space + "!" + errcode);
			}
			if(errcode == 9) {
				System.out.println("Error : 程序结尾丢了句号�� + space + "!" + errcode);
				PL0.fa1.println("Error : 程序结尾丢了句号�� + space + "!" + errcode);
			}
			if(errcode == 10) {
				System.out.println("Error : 语句之间漏了�� + space + "!" + errcode);
				PL0.fa1.println("Error : 语句之间漏了�� + space + "!" + errcode);
			}
			if(errcode == 11) {
				System.out.println("Error : 标识符未说明" + space + "!" + errcode);
				PL0.fa1.println("Error : 标识符未说明" + space + "!" + errcode);
			}
			if(errcode == 12) {
				System.out.println("Error : 赋值语句中，赋值号左部标识符属性应是变� + space + "!" + errcode);
				PL0.fa1.println("Error : 赋值语句中，赋值号左部标识符属性应是变� + space + "!" + errcode);
			}
			if(errcode == 13) {
				System.out.println("Error : 赋值语句左部标识符后应是赋值号�=� + space + "!" + errcode);
				PL0.fa1.println("Error : 赋值语句左部标识符后应是赋值号�=� + space + "!" + errcode);
			}
			if(errcode == 14) {
				System.out.println("Error : call后应为标识符" + space + "!" + errcode);
				PL0.fa1.println("Error : call后应为标识符" + space + "!" + errcode);
			}
			if(errcode == 15) {
				System.out.println("Error : call后标识符属性应为过� + space + "!" + errcode);
				PL0.fa1.println("Error : call后标识符属性应为过� + space + "!" + errcode);
			}
			if(errcode == 16) {
				System.out.println("Error : 条件语句中丢了“then� + space + "!" + errcode);
				PL0.fa1.println("Error : 条件语句中丢了“then� + space + "!" + errcode);
			}
			if(errcode == 17) {
				System.out.println("Error : 丢了“end”或�� + space + "!" + errcode);
				PL0.fa1.println("Error : 丢了“end”或�� + space + "!" + errcode);
			}
			if(errcode == 18) {
				System.out.println("Error : while型循环语句中丢了“do� + space + "!" + errcode);
				PL0.fa1.println("Error : while型循环语句中丢了“do� + space + "!" + errcode);
			}
			if(errcode == 19) {
				System.out.println("Error : 语句后的符号不正� + space + "!" + errcode);
				PL0.fa1.println("Error : 语句后的符号不正� + space + "!" + errcode);
			}
			if(errcode == 20) {
				System.out.println("Error : 应为关系运算� + space + "!" + errcode);
				PL0.fa1.println("Error : 应为关系运算� + space + "!" + errcode);
			}
			if(errcode == 21) {
				System.out.println("Error : 表达式内标识符属性不能是过程" + space + "!" + errcode);
				PL0.fa1.println("Error : 表达式内标识符属性不能是过程" + space + "!" + errcode);
			}
			if(errcode == 22) {
				System.out.println("Error : 表达式中漏掉右括号�� + space + "!" + errcode);
				PL0.fa1.println("Error : 表达式中漏掉右括号�� + space + "!" + errcode);
			}
			if(errcode == 23) {
				System.out.println("Error : 因子后的非法符号" + space + "!" + errcode);
				PL0.fa1.println("Error : 因子后的非法符号" + space + "!" + errcode);
			}
			if(errcode == 24) {
				System.out.println("Error : 表达式的开始符不能是此符号" + space + "!" + errcode);
				PL0.fa1.println("Error : 表达式的开始符不能是此符号" + space + "!" + errcode);
			}
			if(errcode == 30) {
				System.out.println("Error : 常数越界" + space + "!" + errcode);
				PL0.fa1.println("Error : 常数越界" + space + "!" + errcode);
			}
			if(errcode == 31) {
				System.out.println("Error : 表达式内常数越界" + space + "!" + errcode);
				PL0.fa1.println("Error : 表达式内常数越界" + space + "!" + errcode);
			}
			if(errcode == 32) {
				System.out.println("Error : 嵌套深度超过允许� + space + "!" + errcode);
				PL0.fa1.println("Error : 嵌套深度超过允许� + space + "!" + errcode);
			}
			if(errcode == 33) {
				System.out.println("Error : read或write或for语句中缺“）� + space + "!" + errcode);
				PL0.fa1.println("Error : read或write或for语句中缺“）� + space + "!" + errcode);
			}
			if(errcode == 34) {
				System.out.println("Error : read或write或for语句中缺“（� + space + "!" + errcode);
				PL0.fa1.println("Error : read或write或for语句中缺“（� + space + "!" + errcode);
			}
			if(errcode == 35) {
				System.out.println("Error : read语句括号中的标识符不是变� + space + "!" + errcode);
				PL0.fa1.println("Error : read语句括号中的标识符不是变� + space + "!" + errcode);
			}

	}
}
