package guiTest;

//A Small BASIC Interpreter.
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

//Exception class for interpreter errors.
class InterpreterException extends Exception {
String errStr; // describes the error
public InterpreterException(String str) {
   errStr = str;
   }
public String toString() {
   return errStr;
   }
}

//The Small BASIC interpreter.
class interpreter {
final int PROG_SIZE = 10000; // maximum program size
// These are the token types.
final int NONE = 0;
final int DELIMITER = 1;
final int VARIABLE = 2;
final int NUMBER = 3;
final int COMMAND = 4;
final int QUOTEDSTR = 5;   //almost string
// These are the types of errors.
final int SYNTAX = 0;
final int UNBALPARENS = 1;
final int NOEXP = 2;
final int DIVBYZERO = 3;
final int EQUALEXPECTED = 4;
final int NOTVAR = 5;
final int NOTSEMICOLON = 6;
final int MISSINGQUOTE = 7;
final int FILENOTFOUND = 8;
final int FILEIOERROR = 9;
final int INPUTIOERROR = 10;
// Internal representation of the Small BASIC keywords.
final int UNKNCOM = 11;
final int PRINT = 12;
final int INPUT = 13;
final int IF = 14;
final int ELIF = 15;
final int ELSE = 16;
final int END = 17;
final int EOL = 18;

// This token indicates end-of-program.
final String EOP = "\0";
// Codes for double-operators, such as <=.
final char LE = 1;
final char GE = 2;
final char NE = 3;
// Array for variables.
private double vars[];
// This class links keywords with their keyword tokens.
class Keyword {
   String keyword; // string form
   int keywordTok; // internal representation
   Keyword(String str, int t) {
      keyword = str;
      keywordTok = t;
   }
}
/* Table of keywords with their internal representation.
All keywords must be entered lowercase. */
Keyword kwTable[] = {
   new Keyword("print", PRINT), // in this table.
   new Keyword("input", INPUT),
   new Keyword("if", IF),
   new Keyword("end", END),
   new Keyword("elif", ELIF),
   new Keyword("else", ELSE),
};
private char[] prog; // refers to program array
private int progIdx; // current index into program
private String token; // holds current token
private int tokType; // holds token's type
private int kwToken; // internal representation of a keyword
// Support for FOR loops.
class ForInfo {
   int var; // for안에 들어가는 조건문
   double target; // value 계산문
   int loc; // for문같은경우에 시작점
}


private Stack IfStack;  // If를 위한 필요할지도모르는스택
private int left_var;  // if좌측에 저장할 스트링.


// Defines label table entries.
class Label {
   String name; // label
   int loc; // index of label's location in source file
   public Label(String n, int i) {
      name = n;
      loc = i;
   }
}
// A map for labels.
private TreeMap<Integer, String> CommandTable;
ArrayList LineTable = new ArrayList();
// Relational operators.
char rops[] = {
GE, NE, LE, '<', '>', '=', 0
};
/* Create a string containing the relational
operators in order to make checking for
them more convenient. */
String relops = new String(rops);
// Constructor for SBasic.
public interpreter(String progName) throws InterpreterException {
   char tempbuf[] = new char[PROG_SIZE];
   int size;
   // Load the program to execute.
   size = loadProgram(tempbuf, progName);
   if(size != -1) {
   // Create a properly sized array to hold the program.
   prog = new char[size];
   // Copy the program into program array.
   System.arraycopy(tempbuf, 0, prog, 0, size);
   }
}
// Load a program.
private int loadProgram(char[] p, String fname) throws InterpreterException
{
   int size = 0;
   try {
      FileReader fr = new FileReader(fname);
      BufferedReader br = new BufferedReader(fr);
      size = br.read(p, 0, PROG_SIZE);
      fr.close();
   } catch(FileNotFoundException exc) {
      handleErr(FILENOTFOUND);
   } catch(IOException exc) {
      handleErr(FILEIOERROR);
   }
   // If file ends with an EOF mark, back up.
   if(p[size-1] == (char) 26) size--;
   return size; // return size of program
}
// Execute the program.
public void run() throws InterpreterException {
   // Initialize for new program run.
   vars = new double[26];
   CommandTable = new TreeMap<Integer,String>();//TreeMap생성
   progIdx = 0;
   scanLines(); // find the labels in the program
   scanCommands(); // find the labels in the program
   sbInterp(); // execute
}
// Entry point for the Small BASIC interpreter.
private void sbInterp() throws InterpreterException
{
   // This is the interpreter's main loop.
   do {
      getToken();
   // Check for assignment statement.
   if(tokType==VARIABLE) {
      putBack(); // return the var to the input stream
      assignment(); // handle assignment statement
   }
   else // is keyword
   switch(kwToken) {
      case PRINT:
         print();
         break;
      case IF:
         execIf();
         break; 
      case ELIF:
         execELIF();
         break;
      case ELSE:
         execELSE();
         break;
      case INPUT:
         input();
         break;
      case END:
         return;
      
      }
   } while (!token.equals(EOP));
}
// Find all labels.
private void scanLines() throws InterpreterException
{
   int i = 0;
   LineTable.add(0);
   do {
       getToken();

       if(token.equals("\r\n")){// must be a line number
          getToken();
          if(!token.equals("\t")) {
        	 putBack();
             LineTable.add(progIdx);
          }
       }
    } while(!token.equals(EOP));
    progIdx = 0; // reset index to start of program
}
private void scanCommands() throws InterpreterException {
   do {
        getToken();
         if(tokType == COMMAND) {// must be a line number
            //putBack();
            CommandTable.put(progIdx,token); 
         }
      } while(!token.equals(EOP));
      progIdx = 0; // reset index to start of program
}
// Find the start of the next line.
private void findEOL()
{
   while(progIdx < prog.length &&
   prog[progIdx] != '\n') ++progIdx;
   if(progIdx < prog.length) progIdx++;
}
// Assign a variable a value.
private void assignment() throws InterpreterException
{
   int var;
   double value;
   char vname;
   // Get the variable name.
   getToken();
   vname = token.charAt(0);
    if(!Character.isLetter(vname)) {
      handleErr(NOTVAR);
      return;
   }
   // Convert to index into variable table.
   var = (int) Character.toUpperCase(vname) - 'A';
   // Get the equal sign.
   getToken();
   if(!token.equals("=")) {
      handleErr(EQUALEXPECTED);
      return;
   }
   // Get the value to assign.
   getToken();
   if(tokType == COMMAND) {
      left_var = var;
      putBack();
      return;
   }
   putBack();
   value = evaluate();
   // Assign the value.
   vars[var] = value;
}
// Execute a simple version of the PRINT statement.
private void print() throws InterpreterException
{
   double result;
   int len=0, spaces;
   String lastDelim = "";
   getToken(); // get next list item
   if(!token.equals("(")) {
      handleErr(SYNTAX);
      return;
   }
   do {
      getToken(); // get next list item
      if(kwToken==EOL || token.equals(EOP)) break;
      
      if(tokType==QUOTEDSTR) { // is string
         System.out.print(token);
         len += token.length();
         getToken();
      }
      else { // is expression
         putBack();
         result = evaluate();
         getToken();
         System.out.print(result);
         // Add length of output to running total.
         Double t = new Double(result);
         len += t.toString().length(); // save length
      }
      lastDelim = token;
      // If comma, move to next tab stop.
      if(lastDelim.equals(",")) {
         // compute number of spaces to move to next tab
         spaces = 4 - (len % 4);
         len += spaces; // add in the tabbing position
         while(spaces != 0) {
	         System.out.print(" ");
	         spaces--;
         }
      }

   } while (lastDelim.equals(","));
   
   if(kwToken==EOL || token.equals(")")) {
      if(!lastDelim.equals(";") && !lastDelim.equals(","))
         System.out.println();
   }
   else handleErr(SYNTAX);
}

// Execute an IF statement.
//거짓이면 들여쓰기한곳 들은 넘어감.
private void execIf() throws InterpreterException
{
   double result;
   result = evaluate(); // get value of expression
   if(result == 0.0) {
      for(int i=0; i< LineTable.size(); i++) {      
         if((int)LineTable.get(i) > progIdx) {
            progIdx = (int)LineTable.get(i);
            break;
         }
      }
   }
   else findEOL();
}


private void execELIF() throws InterpreterException{
   double result;
   int tempIdx;
   boolean go_elif = false;
   boolean in_Elif_area = false;
   tempIdx = progIdx;
   for(Integer key : CommandTable.keySet()) {
	   if(key >= tempIdx) {break;}
       if(CommandTable.get(key).equals("IF") || (CommandTable.get(key).equals("ELIF"))) {
    	   go_elif = false;		//조건문이 참일때 false
    	   progIdx = key;
    	   result = evaluate();
    	   if(result == 0.0) {
    		   go_elif = true;	//조건문이 거짓일때 true(elif를 성공하기 위해)
    	   }
       }

   }
   progIdx = tempIdx;
   result = evaluate();
   if(!(go_elif == true && result == 1.0)) {  //이 elif 조건이 거짓이면 다음으로 넘어감.
	   for(int i=0; i< LineTable.size(); i++) {      
		   if((int)LineTable.get(i) > progIdx) {
	    	  	progIdx = (int)LineTable.get(i);
	          	break;
	       }
	   }
   }
   else findEOL(); // find start of next line
}

private void execELSE() throws InterpreterException{
	double result;
	int tempIdx;
	boolean go_else = false;
	boolean in_Elif_area = false;
	tempIdx = progIdx;
	for(Integer key : CommandTable.keySet()) {
		if(key >= tempIdx) {break;}
	    if(CommandTable.get(key).equals("IF") || (CommandTable.get(key).equals("ELIF"))) {
	    	go_else = false;		//조건문이 참일때 false
	    	progIdx = key;
	    	result = evaluate();
	    	if(result == 0.0) {
	    		go_else = true;	//조건문이 거짓일때 true(elif를 성공하기 위해)
	    	}
	    }

	}
	progIdx = tempIdx;
	if(go_else == false) {
		for(int i=0; i< LineTable.size(); i++) {      
			if((int)LineTable.get(i) > progIdx) {
		    	 progIdx = (int)LineTable.get(i);
		         break;
		    }
		}
	}
	else findEOL(); // find start of next line
}

// Execute a simple form of INPUT.
private void input() throws InterpreterException
{
   int var;
   double val = 0.0;
   String str;
   BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
   getToken();
   if(!token.equals("(")){
      handleErr(NOTSEMICOLON);
       return;
   }
   getToken();
   if(tokType == QUOTEDSTR) {
      System.out.print(token);
      getToken();
      if(!token.equals(")")) 
        handleErr(NOTSEMICOLON);
   }
   else handleErr(SYNTAX); // otherwise, prompt with ?
   var = left_var;
   try {
      str = br.readLine();
      val = Double.parseDouble(str); // read the value
   } catch (IOException exc) {
      handleErr(INPUTIOERROR);
   } catch (NumberFormatException exc) {
      System.out.println("Invalid input.");
   }
   vars[var] = val; // store it
}

// **************** Expression Parser ****************
// Parser entry point.
private double evaluate() throws InterpreterException
{
   double result = 0.0;
   getToken();
   if(token.equals(EOP))
      handleErr(NOEXP); // no expression present
   // Parse and evaluate the expression.
   result = evalExp1();
   putBack();
   return result;
}
// Process relational operators.
private double evalExp1() throws InterpreterException
{
   double l_temp, r_temp, result;
   char op;
   result = evalExp2();
   // If at end of program, return.
   if(token.equals(EOP)) return result;
   op = token.charAt(0);
   if(isRelop(op)) {
      l_temp = result;
      getToken();
      r_temp = evalExp1();
      switch(op) { // perform the relational operation
         case '<':
         if(l_temp < r_temp) result = 1.0;
         else result = 0.0;
         break;
         case LE:
         if(l_temp <= r_temp) result = 1.0;
         else result = 0.0;
         break;
         case '>':
         if(l_temp > r_temp) result = 1.0;
         else result = 0.0;
         break;
         case GE:
         if(l_temp >= r_temp) result = 1.0;
         else result = 0.0;
         break;
         case '=':
         if(l_temp == r_temp) result = 1.0;
         else result = 0.0;
         break;
         case NE:
         if(l_temp != r_temp) result = 1.0;
         else result = 0.0;
         break;
      }
   }
   return result;
}
// Add or subtract two terms.
private double evalExp2() throws InterpreterException
{
   char op;
   double result;
   double partialResult;
   result = evalExp3();
   while((op = token.charAt(0)) == '+' || op == '-') {
      getToken();
      partialResult = evalExp3();
      switch(op) {
         case '-':
            result = result - partialResult;
            break;
         case '+':
            result = result + partialResult;
            break;
         }
   }
   return result;
}
// Multiply or divide two factors.
private double evalExp3() throws InterpreterException
{
   char op;
   double result;
   double partialResult;
   result = evalExp4();
   while((op = token.charAt(0)) == '*' ||
      op == '/' || op == '%') {
      getToken();
      partialResult = evalExp4();
      switch(op) {
         case '*':
            result = result * partialResult;
            break;
         case '/':
            if(partialResult == 0.0)
            handleErr(DIVBYZERO);
            result = result / partialResult;
            break;
         case '%':
            if(partialResult == 0.0)
            handleErr(DIVBYZERO);
            result = result % partialResult;
            break;
      }
   }
   return result;
}
// Process an exponent.
private double evalExp4() throws InterpreterException
{
   double result;
   double partialResult;
   double ex;
   int t;
   result = evalExp5();
   if(token.equals("^")) {
      getToken();
      partialResult = evalExp4();
      ex = result;
      if(partialResult == 0.0) {
         result = 1.0;
      } else
      for(t=(int)partialResult-1; t > 0; t--)
      result = result * ex;
   }
   return result;
}
// Evaluate a unary + or -.
private double evalExp5() throws InterpreterException
{
   double result;
   String op;
   op = "";
   if((tokType == DELIMITER) && token.equals("+") || token.equals("-")) {
      op = token;
      getToken();
   }
   result = evalExp6();
   if(op.equals("-"))
      result = -result;
   return result;
}
// Process a parenthesized expression.
private double evalExp6() throws InterpreterException
{
   double result;
   if(token.equals("(")) {
      getToken();
      result = evalExp2();
      if(!token.equals(")"))
         handleErr(UNBALPARENS);
      getToken();
   }
   else result = atom();
   return result;
}
// Get the value of a number or variable.
private double atom() throws InterpreterException
{
   double result = 0.0;
   switch(tokType) {
      case NUMBER:
      try {
         result = Double.parseDouble(token);
      } catch (NumberFormatException exc) {
         handleErr(SYNTAX);
         }
         getToken();
         break;
      case VARIABLE:
         result = findVar(token);
         getToken();
         break;
      case COMMAND:
         break;
      default:
         handleErr(SYNTAX);
         break;
   }
   return result;
}
// Return the value of a variable.
private double findVar(String vname) throws InterpreterException
{
   if(!Character.isLetter(vname.charAt(0))){
      handleErr(SYNTAX);
      return 0.0;
   }
   return vars[Character.toUpperCase(vname.charAt(0))-'A'];
}
// Return a token to the input stream.
private void putBack()
{
   if(token == EOP) return;
   for(int i=0; i < token.length(); i++) progIdx--;
}
// Handle an error.
private void handleErr(int error) throws InterpreterException
{
   String[] err = {
   "Syntax Error",
   "Unbalanced Parentheses",
   "No Expression Present",
   "Division by Zero",
   "Equal sign expected",
   "Not a variable",
   "Label table full",
   "Duplicate label",
   "Undefined label",
   "if뒤에 세미콜론이 없음",
   "들여쓰기 안함",
   "앞에 if를안써서 elif못씀",
   "앞에 if나 elif를안써서 else못씀",
   "Closing quotes needed",
   "File not found",
   "I/O error while loading file",
   "I/O error on INPUT statement"
   };
   throw new InterpreterException(err[error]);
}
// Obtain the next token.
private void getToken() throws InterpreterException
{
   char ch;
   tokType = NONE;
   token = "";
   kwToken = UNKNCOM;
   // Check for end of program.
   if(progIdx == prog.length) {
      token = EOP;
      return;
   }
   // Skip over white space.
   while(progIdx < prog.length && isSpace(prog[progIdx])) 
      progIdx++;
   // Trailing whitespace ends program.
   if(progIdx == prog.length) {
      token = EOP;
      tokType = DELIMITER;
      return;
   }
   if(prog[progIdx] == '\r') { // 엔터
      progIdx += 2;
      kwToken = EOL;
      token = "\r\n";
      return;
   }
   if(isTab(prog[progIdx])) { // 들여쓰기
      while(progIdx < prog.length && isTab(prog[progIdx])) 
         progIdx++;
      kwToken = QUOTEDSTR;
      token = "\t";
      return;
   }
   // Check for relational operator.
   ch = prog[progIdx];
   if(ch == '<' || ch == '>') {
      if(progIdx+1 == prog.length) handleErr(SYNTAX);
      switch(ch) {
         case '<':
            if(prog[progIdx+1] == '>') {
               progIdx += 2;;
               token = String.valueOf(NE);
            }
            else if(prog[progIdx+1] == '=') {
               progIdx += 2;
               token = String.valueOf(LE);
            }
            else {
               progIdx++;
               token = "<";
            }
            break;
         case '>':
            if(prog[progIdx+1] == '=') {
               progIdx += 2;;
               token = String.valueOf(GE);
            }
         else {
            progIdx++;
            token = ">";
         }
         break;
      }
      tokType = DELIMITER;
      return;
   }
   if(isDelim(prog[progIdx])) {
      // Is an operator.
      token += prog[progIdx];
      progIdx++;
      tokType = DELIMITER;
   }
   else if(Character.isLetter(prog[progIdx])) {
      // Is a variable or keyword.
      while(!isDelim(prog[progIdx])) {
         token += prog[progIdx];
         progIdx++;
         if(progIdx >= prog.length) break;
      }
      kwToken = lookUp(token);
      if(kwToken==UNKNCOM) tokType = VARIABLE;
      else tokType = COMMAND;
   }
   else if(Character.isDigit(prog[progIdx])) {
      // Is a number.
      while(!isDelim(prog[progIdx])) {
         token += prog[progIdx];
         progIdx++;
         if(progIdx >= prog.length)
            break;
      }
      tokType = NUMBER;
   }
   else if(prog[progIdx] == '"') {
      // Is a quoted string.
      progIdx++;
      ch = prog[progIdx];
      while(ch !='"' && ch != '\r') {
         token += ch;
         progIdx++;
         ch = prog[progIdx];
      }
      if(ch == '\r') handleErr(MISSINGQUOTE);
      progIdx++;
      tokType = QUOTEDSTR;
   }
   else { // unknown character terminates program
      token = EOP;
      return;
   }
}
// Return true if c is a delimiter.
private boolean isDelim(char c)
{
   if((" \r,;:<>+-/*%^=()".indexOf(c) != -1))
   return true;
   return false;
}
// Return true if c is a space or a tab.
boolean isSpace(char c)
{
   if(c == ' ') return true;
   return false;
}
boolean isTab(char c)
{
   if(c =='\t') return true;
   return false;
}
// Return true if c is a relational operator.
boolean isRelop(char c) {
   if(relops.indexOf(c) != -1) return true;
   return false;
}
/* Look up a token's internal representation in the
token table. */
private int lookUp(String s)
{
   int i;
   // Convert to lowercase.
   s = s.toLowerCase();
   // See if token is in table.
   for(i=0; i < kwTable.length; i++)
      if(kwTable[i].keyword.equals(s))
         return kwTable[i].keywordTok;
   return UNKNCOM; // unknown keyword
}
}

public class simple_python{
   public static void main(String args[]) {
      /*if (args.length != 1) {
         System.out.println("Usage: sbasic <filename>");
         return;
      }*/
      try {
         interpreter i = new interpreter("SBTEST.txt");
         i.run();
      }catch(InterpreterException exc) {
         System.out.println(exc);
      }
   }
}