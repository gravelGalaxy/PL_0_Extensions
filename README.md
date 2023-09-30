# PL/0语言扩展

#### 开发工具

IDEA 2022.1.4

#### 运行环境 

java 14+

#### 新增功能：

1. 新增for  

   ```java
   for(AssignStatement ; condition ; i++)
   	statement
   ```

2. 新增++、--

   前置后置++、--，可用于语句、for、write

3. 新增+=、-=、*=、/=

   +=、-=、*=、/= 可跟变量、常量、表达式

4. 新增！、~、%

   ！：逻辑取反
   ~：数值取反
   %：取余

5. 新增else

   ```
   if condition
   then
   statement
   else
   statement
   ```

6. 新增writeCh

   ```
   writeCh( ASCII码 );
   ```

7. 注释

   ```
   /* 内容 */
   ```

8. 报错的汉字提示


​		