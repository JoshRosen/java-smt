/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.solver.visitors;

import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.FormulaManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base class for visitors for boolean formulas that traverse recursively
 * through the formula and somehow transform it (i.e., return a boolean formula).
 * This class ensures that each identical subtree of the formula
 * is visited only once to avoid the exponential explosion.
 * When a subclass wants to traverse into a subtree of the formula,
 * it needs to call {@link #visitIfNotSeen(BooleanFormula)} or
 * {@link #visitIfNotSeen(BooleanFormula...)} to ensure this.
 *
 * <p>
 * By default this class implements the identity function.
 * </p>
 *
 * <p>
 * No guarantee on iteration order is made.
 * </p>
 */
public abstract class BooleanFormulaTransformationVisitor
    extends BooleanFormulaVisitor<BooleanFormula> {

  private final BooleanFormulaManager bfmgr;

  private final Map<BooleanFormula, BooleanFormula> cache;

  protected BooleanFormulaTransformationVisitor(
      FormulaManager pFmgr, Map<BooleanFormula, BooleanFormula> pCache) {
    super(pFmgr);
    bfmgr = pFmgr.getBooleanFormulaManager();
    cache = pCache;
  }

  protected final BooleanFormula visitIfNotSeen(BooleanFormula f) {
    BooleanFormula out = cache.get(f);
    if (out == null) {
      out = super.visit(f);
      cache.put(f, out);
    }
    return out;
  }

  protected final List<BooleanFormula> visitIfNotSeen(BooleanFormula... pOperands) {
    List<BooleanFormula> args = new ArrayList<>(pOperands.length);
    for (BooleanFormula arg : pOperands) {
      args.add(visitIfNotSeen(arg));
    }
    return args;
  }

  @Override
  protected BooleanFormula visitTrue() {
    return bfmgr.makeBoolean(true);
  }

  @Override
  protected BooleanFormula visitFalse() {
    return bfmgr.makeBoolean(false);
  }

  @Override
  protected BooleanFormula visitAtom(BooleanFormula pAtom) {
    return pAtom;
  }

  @Override
  protected BooleanFormula visitNot(BooleanFormula pOperand) {
    return bfmgr.not(visitIfNotSeen(pOperand));
  }

  @Override
  protected BooleanFormula visitAnd(BooleanFormula... pOperands) {
    return bfmgr.and(visitIfNotSeen(pOperands));
  }

  @Override
  protected BooleanFormula visitOr(BooleanFormula... pOperands) {
    return bfmgr.or(visitIfNotSeen(pOperands));
  }

  @Override
  protected BooleanFormula visitEquivalence(BooleanFormula pOperand1, BooleanFormula pOperand2) {
    return bfmgr.equivalence(visitIfNotSeen(pOperand1), visitIfNotSeen(pOperand2));
  }

  @Override
  protected BooleanFormula visitImplication(BooleanFormula pOperand1, BooleanFormula pOperand2) {
    return bfmgr.implication(visitIfNotSeen(pOperand1), visitIfNotSeen(pOperand2));
  }

  @Override
  protected BooleanFormula visitIfThenElse(
      BooleanFormula pCondition, BooleanFormula pThenFormula, BooleanFormula pElseFormula) {
    return bfmgr.ifThenElse(
        visitIfNotSeen(pCondition), visitIfNotSeen(pThenFormula), visitIfNotSeen(pElseFormula));
  }
}