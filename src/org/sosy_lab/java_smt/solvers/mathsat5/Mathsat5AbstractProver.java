/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.java_smt.solvers.mathsat5;

import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_check_sat;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_create_config;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_destroy_config;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_destroy_env;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_free_termination_test;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_pop_backtrack_point;
import static org.sosy_lab.java_smt.solvers.mathsat5.Mathsat5NativeApi.msat_set_option_checked;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.SolverException;

/** Common base class for {@link Mathsat5TheoremProver} and {@link Mathsat5InterpolatingProver}. */
abstract class Mathsat5AbstractProver<T2> implements BasicProverEnvironment<T2> {

  protected final Mathsat5SolverContext context;
  protected final long curEnv;
  private final long curConfig;
  private final long terminationTest;
  protected final Mathsat5FormulaCreator creator;
  protected boolean closed = false;

  protected Mathsat5AbstractProver(
      Mathsat5SolverContext pContext, Map<String, String> pConfig, Mathsat5FormulaCreator creator) {
    context = pContext;
    this.creator = creator;
    curConfig = buildConfig(pConfig);
    curEnv = context.createEnvironment(curConfig);
    terminationTest = context.addTerminationTest(curEnv);
  }

  private long buildConfig(Map<String, String> pConfig) {
    long cfg = msat_create_config();
    for (Entry<String, String> entry : pConfig.entrySet()) {
      msat_set_option_checked(cfg, entry.getKey(), entry.getValue());
    }
    return cfg;
  }

  @Override
  public boolean isUnsat() throws InterruptedException, SolverException {
    Preconditions.checkState(!closed);
    return !msat_check_sat(curEnv);
  }

  @Override
  public Model getModel() throws SolverException {
    Preconditions.checkState(!closed);
    return new Mathsat5Model(getMsatModel(), creator);
  }

  @Override
  public ImmutableList<ValueAssignment> getModelAssignments() throws SolverException {
    try (Mathsat5Model model = new Mathsat5Model(getMsatModel(), creator)) {
      return model.modelToList();
    }
  }

  /** @throws SolverException if an expected MathSAT failure occurs */
  protected long getMsatModel() throws SolverException {
    return Mathsat5NativeApi.msat_get_model(curEnv);
  }

  @Override
  public void pop() {
    Preconditions.checkState(!closed);
    msat_pop_backtrack_point(curEnv);
  }

  @Override
  public void close() {
    Preconditions.checkState(!closed);
    msat_destroy_env(curEnv);
    msat_free_termination_test(terminationTest);
    msat_destroy_config(curConfig);
    closed = true;
  }
}
