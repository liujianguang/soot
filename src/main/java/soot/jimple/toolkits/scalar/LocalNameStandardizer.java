/* Soot - a J*va Optimization Framework
 * Copyright (C) 1997-1999 Raja Vallee-Rai
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package soot.jimple.toolkits.scalar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.ErroneousType;
import soot.FloatType;
import soot.G;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.NullType;
import soot.PhaseOptions;
import soot.ShortType;
import soot.Singletons;
import soot.StmtAddressType;
import soot.Type;
import soot.UnknownType;
import soot.Value;
import soot.ValueBox;
import soot.util.Chain;

public class LocalNameStandardizer extends BodyTransformer {
  public LocalNameStandardizer(Singletons.Global g) {
  }

  public static LocalNameStandardizer v() {
    return G.v().soot_jimple_toolkits_scalar_LocalNameStandardizer();
  }

  @Override
  protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
    boolean onlyStackName = PhaseOptions.getBoolean(options, "only-stack-locals");
    boolean sortLocals = PhaseOptions.getBoolean(options, "sort-locals");
    final BooleanType booleanType = BooleanType.v();
    final ByteType byteType = ByteType.v();
    final ShortType shortType = ShortType.v();
    final CharType charType = CharType.v();
    final IntType intType = IntType.v();
    final LongType longType = LongType.v();
    final DoubleType doubleType = DoubleType.v();
    final FloatType floatType = FloatType.v();
    final ErroneousType erroneousType = ErroneousType.v();
    final UnknownType unknownType = UnknownType.v();
    final StmtAddressType stmtAddressType = StmtAddressType.v();
    final NullType nullType = NullType.v();

    // Change the names to the standard forms now.
    {
      int objectCount = 0;
      int intCount = 0;
      int longCount = 0;
      int floatCount = 0;
      int doubleCount = 0;
      int addressCount = 0;
      int errorCount = 0;
      int nullCount = 0;

      /*
       * The goal of this option is to ensure that local ordering remains consistent between different iterations of soot.
       * This helps to ensure things like stable string representations of instructions and stable jimple representations of
       * a methods body when soot is used to load the same code in different iterations.
       *
       * First sorts the locals alphabetically by the string representation of their type. Then if there are two locals with
       * the same type, it uses the only other source of structurally stable information (i.e. the instructions themselves)
       * to produce an ordering for the locals that remains consistent between different soot instances. It achieves this by
       * determining the position of a local's first occurrence in the instruction's list of definition statements. This
       * position is then used to sort the locals with the same type in an ascending order.
       *
       * The only times that this may not produce a consistent ordering for the locals between different soot instances is if
       * a local is never defined in the instructions or if the instructions themselves are changed in some way that effects
       * the ordering of the locals. In the first case, if a local is never defined, the other jimple body phases will remove
       * this local as it is unused. As such, all we have to do is rerun this LocalNameStandardizer after all other jimple
       * body phases to eliminate any ambiguity introduced by these phases and by the removed unused locals. In the second
       * case, if the instructions themselves changed then the user would have had to intentionally told soot to modify the
       * instructions of the code. Otherwise, the instructions would not have changed because we assume the instructions to
       * always be structurally stable between different instances of soot. As such, in this instance, the user should not be
       * expecting soot to produce the same output as the input and thus the ordering of the locals does not matter.
       */
      if (sortLocals) {
        Chain<Local> locals = body.getLocals();
        final List<ValueBox> defs = body.getDefBoxes();
        ArrayList<Local> sortedLocals = new ArrayList<Local>(locals);

        Collections.sort(sortedLocals, new Comparator<Local>() {
          private Map<Local, Integer> firstOccuranceCache = new HashMap<Local, Integer>();

          @Override
          public int compare(Local arg0, Local arg1) {
            int ret = arg0.getType().toString().compareTo(arg1.getType().toString());
            if (ret == 0) {
              ret = Integer.compare(getFirstOccurance(arg0), getFirstOccurance(arg1));
            }
            return ret;
          }

          private int getFirstOccurance(Local l) {
            Integer cur = firstOccuranceCache.get(l);
            if (cur != null) {
              return cur;
            } else {
              int count = 0;
              int first = -1;
              for (ValueBox vb : defs) {
                Value v = vb.getValue();
                if (v instanceof Local && v.equals(l)) {
                  first = count;
                  break;
                }
                count++;
              }
              firstOccuranceCache.put(l, first);
              return first;
            }
          }
        });
        locals.clear();
        locals.addAll(sortedLocals);
      }

      for (Local l : body.getLocals()) {
        String prefix = "";

        if (l.getName().startsWith("$")) {
          prefix = "$";
        } else {
          if (onlyStackName) {
            continue;
          }
        }

        final Type type = l.getType();

        if (type.equals(booleanType)) {
          l.setName(prefix + "z" + intCount++);
        } else if (type.equals(byteType)) {
          l.setName(prefix + "b" + longCount++);
        } else if (type.equals(shortType)) {
          l.setName(prefix + "s" + longCount++);
        } else if (type.equals(charType)) {
          l.setName(prefix + "c" + longCount++);
        } else if (type.equals(intType)) {
          l.setName(prefix + "i" + longCount++);
        } else if (type.equals(longType)) {
          l.setName(prefix + "l" + longCount++);
        } else if (type.equals(doubleType)) {
          l.setName(prefix + "d" + doubleCount++);
        } else if (type.equals(floatType)) {
          l.setName(prefix + "f" + floatCount++);
        } else if (type.equals(stmtAddressType)) {
          l.setName(prefix + "a" + addressCount++);
        } else if (type.equals(erroneousType) || type.equals(unknownType)) {
          l.setName(prefix + "e" + errorCount++);
        } else if (type.equals(nullType)) {
          l.setName(prefix + "n" + nullCount++);
        } else {
          l.setName(prefix + "r" + objectCount++);
        }
      }
    }
  }
}
