// [expunged]
// Period.
// "Simple" Calculator

import java.util.Scanner;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main {
  public static final boolean PRINT_DEBUG = false;
  public static boolean EXPLICIT_PARENS = false;

  static class StringWrapper {
    String s;
    int i = 0;
    Stack<Integer> stack = new Stack<>();
    StringWrapper(String s) {
      this.s = s;
    }
    Character next() {
      if (i < this.s.length())
        return this.s.charAt(i++);
      i++;
      return '\0';
    }
    void push() {
      this.i--;
    }
    void mark() {
      this.stack.push(this.i);
    }
    void unmark() {
      this.stack.pop();
    }
    void restore() {
      this.i = this.stack.pop();
    }
    @Override
    public String toString() {
      return this.s.substring(this.i);
    }
  }

  interface Evaluable {
    Evaluable parse(StringWrapper input);
    Double evaluate();
    String toString();
  }

  public static class ParseFailedException extends RuntimeException {
    public ParseFailedException(String s) {
      super(s);
    }
  }

  static class Numeral implements Evaluable {
    String integerSign;
    String integer;
    String decimal;
    String exponentiationSign;
    String exponentiation;

    public Numeral parse(StringWrapper input) {
      input.mark();
      Character curr = input.next();
      while (isWhitespace(curr))
        curr = input.next();

      // Integer section "-123" of "-123.456E+789"
      integerSign = "+";
      if (curr == '+' || curr == '-') {
        integerSign = curr.toString();
        curr = input.next();
      }
      while ('0' <= curr && curr <= '9') {
        if (integer == null)
          integer = "";
        integer += curr;
        curr = input.next();
      }

      // No more sections?
      if (curr != '.' && curr != 'e' && curr != 'E') {
        if (integer != null) {
          input.push();
          input.unmark();
          return this;
        } else {
          // Nothing found
          input.restore();
          return null;
        }
      }

      // Decimal section ".456" of "123.456E+789"
      if (curr == '.') {
        curr = input.next();
        while ('0' <= curr && curr <= '9') {
          if (decimal == null)
            decimal = "";
          decimal += curr;
          curr = input.next();
        }
      }
      // Invalid reading
      if (integer == null && decimal == null) {
        input.restore();
        throw new ParseFailedException("Found decimal or exponentiation but no actual numeral.");
      }
      // No more sections
      if (curr != 'e' && curr != 'E') {
        input.push();
        input.unmark();
        return this;
      }

      // Exponentiation section "E+789" of "123.456E+789"
      curr = input.next();
      exponentiationSign = "+";
      if (curr == '+' || curr == '-') {
        exponentiationSign = curr.toString();
        curr = input.next();
      }
      while ('0' <= curr && curr <= '9') {
        if (exponentiation == null)
          exponentiation = "";
        exponentiation += curr;
        curr = input.next();
      }

      if (exponentiation == null) {
        input.restore();
        throw new ParseFailedException("Found exponentiation marker, but no numeral after.");
      }
      input.push();
      input.unmark();
      return this;
    }
    public Double evaluate() {
      String retStr = integerSign + (integer == null ? "0" : integer);
      if (decimal != null)
        retStr += "." + decimal;
      double retDbl = Double.parseDouble(retStr);
      if (exponentiation != null) {
        int pow = Integer.parseInt(exponentiation);
        double factor = exponentiationSign.equals("-") ? .1 : 10;
        while (pow-- > 0)
          retDbl *= factor;
      }
      return retDbl;
    }
    @Override
    public String toString() {
      String ret = "";
      if (!integerSign.equals("+"))
        ret = integerSign;
      if (integer != null)
        ret += integer;
      if (decimal != null)
        ret += "." + decimal;
      if (exponentiation != null) {
        ret += "E";
        if (!exponentiationSign.equals("+"))
          ret += exponentiationSign;
        ret += exponentiation;
      }
      return EXPLICIT_PARENS?"("+ret+")":ret;
    }

    public static void test() {
      assert new Numeral().parse(new StringWrapper("55")).evaluate() == 55;
      assert new Numeral().parse(new StringWrapper("55.")).evaluate() == 55;
      assert new Numeral().parse(new StringWrapper("55e1")).evaluate() == 550;
      assert new Numeral().parse(new StringWrapper("55e+3")).evaluate() == 55000;
      assert new Numeral().parse(new StringWrapper("55E-2")).evaluate() == .55;
      assert new Numeral().parse(new StringWrapper("55e+2")).evaluate() == 5500;
      assert new Numeral().parse(new StringWrapper("123.987")).evaluate() == 123.987;
      assert Math.abs(new Numeral().parse(new StringWrapper("1.23456e5")).evaluate() / 1.23456e5 - 1) < 1e-5;
      // Doesn't look like a number (other symbol) should return null
      assert new Numeral().parse(new StringWrapper("()")) == null;
      assert new Numeral().parse(new StringWrapper("/ 5")) == null;
      assert new Numeral().parse(new StringWrapper("+")) == null;
      assert new Numeral().parse(new StringWrapper("-")) == null;
      // Looks like a number, but isn't parseable. Should fail.
      try { new Numeral().parse(new StringWrapper(".")); assert false; } catch (ParseFailedException ignored) {}
      try { new Numeral().parse(new StringWrapper("e")); assert false; } catch (ParseFailedException ignored) {}
      try { new Numeral().parse(new StringWrapper("e+")); assert false; } catch (ParseFailedException ignored) {}
      try { new Numeral().parse(new StringWrapper("e10")); assert false; } catch (ParseFailedException ignored) {}
      try { new Numeral().parse(new StringWrapper(".0e")); assert false; } catch (ParseFailedException ignored) {}
      try { new Numeral().parse(new StringWrapper(".e0")); assert false; } catch (ParseFailedException ignored) {}
      try { new Numeral().parse(new StringWrapper("5.2e-")); assert false; } catch (ParseFailedException ignored) {}
      assert new Numeral().parse(new StringWrapper(".1e1")).evaluate() == 1;
      assert new Numeral().parse(new StringWrapper("123;")).evaluate() == 123;
      assert new Numeral().parse(new StringWrapper(".123+456")).evaluate() == .123;
      assert new Numeral().parse(new StringWrapper(".123.456")).evaluate() == .123;
    }
  }

  static class Constant implements Evaluable {
    Double value;
    String str;
    public Constant parse(StringWrapper input) {
      input.mark();
      Character curr = input.next();
      while (isWhitespace(curr))
        curr = input.next();
      input.push();

      String substring = input.s.substring(input.i).toLowerCase();
      if (substring.startsWith("pi")) {
        value = Math.PI;
        str = "pi";
        input.i += 2;
        input.unmark();
        return this;
      }
      if (substring.startsWith("e")) {
        value = Math.E;
        str = "e";
        input.i += 1;
        input.unmark();
        return this;
      }
//      if (substring.startsWith("tau")) {
//        value = Math.TAU;
//        str = "tau";
//        input.i += 3;
//        input.unmark();
//        return this;
//      }
      input.restore();
      return null;
    }
    public Double evaluate() {
      return value;
    }
    public String toString() {
      return EXPLICIT_PARENS?"("+str+")":str;
    }
  }

  static class Grouping implements Evaluable {
    Evaluable inner = null;
    public Grouping parse(StringWrapper input) {
      input.mark();
      Character curr = input.next();
      while (isWhitespace(curr))
        curr = input.next();

      if (curr != '(') {
        input.restore();
        return null;
      }

      inner = new Expression().parse(input);

      if (inner == null) {
        input.restore();
        return null;
      }

      do {
        curr = input.next();
      } while (isWhitespace(curr));

      if (curr != ')') {
        input.restore();
        return null;
      }
      return this;
    }
    public Double evaluate() {
      return inner.evaluate();
    }
    public String toString() {
      return "( " + inner.toString() + " )";
    }
  }

  static class Operator {
    private static Set<String> tokens = new HashSet<>(20);
    final int precedence;
    final String shape;
    final String token;
    final int parametersLeft;
    final int parametersRight;
    final Function<Evaluable[], Double> action;

    final static Operator[] OPS = new Operator[]{
        new Operator(-6, 0, "ln", 1, (arr) -> Math.log(arr[0].evaluate())),
        new Operator(-6, 0, "log", 1, (arr) -> Math.log10(arr[0].evaluate())),
        new Operator(-5, 1, "^", 1, (arr) -> Math.pow(arr[0].evaluate(),arr[1].evaluate())),
        new Operator(-4, 0, "+", 1, (arr) -> arr[0].evaluate()),
        new Operator(-4, 0, "-", 1, (arr) -> -(arr[0].evaluate())),
        new Operator(3, 1, " * ", 1, (arr) -> (arr[0].evaluate()) * (arr[1].evaluate())),
        new Operator(3, 1, " / ", 1, (arr) -> (arr[0].evaluate()) / (arr[1].evaluate())),
        new Operator(3, 1, " % ", 1, (arr) -> (arr[0].evaluate()) % (arr[1].evaluate())),
        new Operator(2, 1, " + ", 1, (arr) -> (arr[0].evaluate()) + (arr[1].evaluate())),
        new Operator(2, 1, " - ", 1, (arr) -> (arr[0].evaluate()) - (arr[1].evaluate())),
        new Operator(1, 1, " mod ", 1, (arr) -> (arr[0].evaluate()) % (arr[1].evaluate())),
        //        new Operator(5, 0,"log_",2, (arr) -> (Math.log(arr[1].evaluate())) / (Math.log(arr[0].evaluate()))),
        //        new Operator("abs(#)", (arr) -> (Math.log(arr[1].evaluate())) / (Math.log(arr[0].evaluate()))),
        //        new Operator("# ? # : #", (arr) -> (arr[0].evaluate()!=0) ? (arr[1].evaluate()) : (arr[2].evaluate()))
    };

    public String toString() {
      return shape;
    }
    String toString(Evaluable[] parameters) {
      StringBuilder ret = new StringBuilder(EXPLICIT_PARENS?"(":"");
      int arg = 0;
      char curr;
      for (int i = 0; i < shape.length(); i++) {
        curr = shape.charAt(i);
        if (curr == '#')
          ret.append(parameters[arg++].toString());
        else
          ret.append(curr);
      }
      ret.append(EXPLICIT_PARENS?")":"");
      return ret.toString();
    }

    public Operator(int precedence, int parametersLeft, String token, int parametersRight, Function<Evaluable[], Double> action) {
      this.precedence = precedence;
      StringBuilder shape = new StringBuilder("# ".repeat(parametersLeft));
      if (parametersLeft > 0)
        shape.deleteCharAt(shape.length() - 1);
      shape.append(token).append("# ".repeat(parametersRight));
      if (parametersRight > 0)
        shape.deleteCharAt(shape.length() - 1);
      this.shape = shape.toString();
      this.token = token.strip();
      tokens.add(this.token);
      this.parametersLeft = parametersLeft;
      this.parametersRight = parametersRight;
      this.action = action;
    }
  }

  static class Operation implements Evaluable {
    Operator op;
    Evaluable[] operands;
    public Operation(Operator op, List<Expression.Union> parameters) {
      this.op = op;
      List<Evaluable> temp = parameters.stream().filter(a -> a instanceof Expression.ExWrap).map(a -> ((Expression.ExWrap) a).wrapped).collect(Collectors.toList());

      if (temp.size() != op.parametersLeft + op.parametersRight) {
        System.out.println("Temp passed: " + temp);
        throw new IllegalArgumentException("Incorrect number of parameters: Expected " + (op.parametersLeft + op.parametersRight) + " got " + temp.size() + " instead.");
      }

      this.operands = new Evaluable[temp.size()];
      for (int i = 0; i < temp.size(); i++)
        this.operands[i] = temp.get(i);
    }
    public Evaluable parse(StringWrapper input) {
      throw new IllegalArgumentException("Use Expression to parse.");
    }
    public Double evaluate() {
      return op.action.apply(operands);
    }
    public String toString() {
      return op.toString(operands);
    }
  }

  static class Expression implements Evaluable {
    List<Union> elements = new ArrayList<>();

    interface Union {}
    static class ExWrap implements Union {
      final Evaluable wrapped;
      public ExWrap(Evaluable ex) {wrapped = ex;}
    }
    static class OpStrWrap implements Union {
      final String wrapped;
      public OpStrWrap(String op) {wrapped = op;}
    }
    static class OpWrap implements Union {
      final Operator wrapped;
      public OpWrap(Operator op) {wrapped = op;}
    }

    static Union wrap(Evaluable ex) {return new ExWrap(ex);}
    static Union wrap(String op) {return new OpStrWrap(op);}
    static Union wrap(Operator op) {return new OpWrap(op);}

    public Expression parse(StringWrapper input) {
      input.mark();
      boolean passed = readMore(input, 30);

      if (passed) {
        input.unmark();
        return this;
      } else {
        input.restore();
        return null;
      }
    }

    /**
     * Read more if possible. Returns true if successfully read another term into expression.
     */
    public boolean readMore(StringWrapper input, int keepReading) {
      Union ret = null;

      Evaluable tempEx = new Grouping().parse(input);
      if (tempEx != null) {
        if (PRINT_DEBUG)
          System.out.println("Grouping found: " + tempEx.toString());
        ret = wrap(tempEx);
      }


      if (ret == null) {
        String substring = input.s.substring(input.i);
        for (String t : Operator.tokens) {
          if (substring.startsWith(t)) {
            ret = wrap(t);
            input.i += t.length();
            if (PRINT_DEBUG)
              System.out.println("Operator found: " + t + " in " + substring);
            break;
          } else {
            //            if (PRINT_DEBUG)
            //              System.out.println("Not " + t + " in " + substring);
          }
        }
      }

      if (ret == null) {
        tempEx = new Constant().parse(input);
        if (tempEx != null) {
          if (PRINT_DEBUG)
            System.out.println("Constant found: " + tempEx.toString());
          ret = wrap(tempEx);
        }
      }

      if (ret == null) {
        tempEx = new Numeral().parse(input);
        if (tempEx != null) {
          if (PRINT_DEBUG)
            System.out.println("Numeral found: " + tempEx.toString());
          ret = wrap(tempEx);
        }
      }

      if (ret == null) {
        if (PRINT_DEBUG)
          System.out.println("Nothing found. Leaving " + input);
        return false;
      }

      elements.add(ret);
      if (PRINT_DEBUG)
        System.out.println("Items found so far: " + this.toString() + ". Leaving \"" + input+"\"");
      while (keepReading-- > 0 && readMore(input, 0)) {}
      return true;
    }
    public Double evaluate() {
      if (elements.isEmpty())
        return null;

      int numOps = 0;
      int opIndex = -1;
      for (int i = 0; i < elements.size(); i++)
        if (elements.get(i) instanceof OpStrWrap) {
          opIndex = i;
          numOps++;
        }
      if (numOps > 0)
        condense();
      if (numOps == 0)
        return ((ExWrap) elements.get(0)).wrapped.evaluate();

      return this.evaluate();
    }
    private boolean condense() {
      // Evaluate shit like "  + 3   -  + - 5    " or "+3-+-5"
      //                    "((+(3)) - (+(-(5))))"

      // leftmost of conflicts -> if can be biop, do biop
      // if must be uniop do uniop.

      boolean wasCondensed = false;
      while (true) {

        // Find all ops
        // Mark all precedences and parameters needed for the operator.
        // Left most op in runs of ops are biops unless leading.
        int numOps = 0;
        List<Integer> opPositions = new ArrayList<>();
        List<List<Operator>> possibleOps = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++)
          if (!(elements.get(i) instanceof ExWrap)) {
            numOps++;
            opPositions.add(i);
          }
        if (numOps == 0)
          return wasCondensed;
        for (int i : opPositions) {
          Union object = elements.get(i);
          if (object instanceof OpWrap)
            possibleOps.add(Collections.singletonList(((OpWrap) object).wrapped));
          else {
            String opToken = ((OpStrWrap)object).wrapped;
            List<Operator> precedence = new ArrayList<>();
            possibleOps.add(precedence);
            for (Operator o : Operator.OPS)
              if (o.token.equals(opToken))
                precedence.add(o);
          }
        }
        // If the first element is an biop/uniop conflict, remove the biop.
        if (opPositions.get(0) == 0 && possibleOps.get(0).size() > 1) {
          if (PRINT_DEBUG) System.out.println("First token is uniop");
          possibleOps.get(0).removeIf(a -> a.parametersLeft != 0); // keep uniop == remove biop
        }
        for (int i = 0; i < opPositions.size(); i++) {
          if (possibleOps.get(i).size() > 1) {
            // first in a run is biop, remainder are uniop
            int pos = opPositions.get(i);
            if (i == 0 || opPositions.get(i - 1) != pos - 1) {
              if (PRINT_DEBUG) System.out.println("First in run is biop");
              possibleOps.get(i).removeIf(a -> a.parametersLeft == 0);
            } else {
              if (PRINT_DEBUG) System.out.println("Rest in run is uniop");
              possibleOps.get(i).removeIf(a -> a.parametersLeft != 0);
            }
            assert possibleOps.get(i).size() == 1;
          }
        }
        List<Operator> ops = new ArrayList<>(possibleOps.size());
        for (int i =0; i<possibleOps.size(); i++) {
          Operator op = possibleOps.get(i).get(0);
          ops.add(op);
          elements.set(opPositions.get(i),wrap(op));
        }
        if (PRINT_DEBUG) System.out.println("Condensing: "+this);

        int maxPrecedence = -1;
        int firstOp = -1;
        for (int i = 0; i < ops.size(); i++) {
          int newVal = Math.abs(ops.get(i).precedence);
          if (newVal >= maxPrecedence) {
            if (maxPrecedence != newVal)
              firstOp = i;
            maxPrecedence = newVal;
            if (ops.get(i).precedence < 0)
              firstOp = i;
          }
        }
        int pos = opPositions.get(firstOp);
        List<Union> toRemove = elements.subList(pos - ops.get(firstOp).parametersLeft, pos + ops.get(firstOp).parametersRight + 1);
        ArrayList<Union> elems = new ArrayList<>(toRemove);
        toRemove.clear();
        elements.add(pos - ops.get(firstOp).parametersLeft, new ExWrap(new Operation(ops.get(firstOp), elems)));

        wasCondensed = true;
      }
    }
    public String toString() {
      StringBuilder ret = new StringBuilder();
      boolean first = true;
      for (Union e : elements) {
        if (!first)
          ret.append(", ");
        first = false;
        if (e instanceof OpStrWrap)
          ret.append('"').append(((OpStrWrap) e).wrapped).append("\"");
        else if (e instanceof ExWrap)
          ret.append(((ExWrap) e).wrapped.toString());
        else if (e instanceof OpWrap)
          ret.append(((OpWrap) e).wrapped);
      }
      return ret.toString();
    }
  }

  static boolean isWhitespace(Character c) {
    return (c != null && (c == ' ' || c == '\t' || c == '\n' || c == '\r'));
  }

  public static void main(String[] args) {
    Numeral.test();
    Scanner input = new Scanner(System.in);

    System.out.print("Enter an expression to parse: ");
    Evaluable expr = new Expression().parse(new StringWrapper(input.next()));
    System.out.println("Tokenized String: " + expr);
    expr.evaluate(); // force cleanup of all internal expressions.
    EXPLICIT_PARENS = true;
    System.out.println("Parsed String: " + expr);
    EXPLICIT_PARENS = false;
    System.out.println("Pretty String: " + expr);
    System.out.println("Evaluates to: " + expr.evaluate());
    System.out.println();

    //    Expression e = new Expression().parse(new StringWrapper("5+pi"));
    //    System.out.println(e.toString());
    //    System.out.println(e.evaluate());
    //    System.out.println(e.toString());
  }
}
