/*
 * Copyright 2003-2005 Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.controlflow;

import com.siyeh.ig.ExpressionInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.ComparisonUtils;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.HardcodedMethodConstants;
import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PointlessIndexOfComparisonInspection extends ExpressionInspection {

    public String getDisplayName() {
        return InspectionGadgetsBundle.message(
                "pointless.indexof.comparison.display.name");
    }

    public String getGroupDisplayName() {
        return GroupNames.CONTROL_FLOW_GROUP_NAME;
    }

    @Nullable
    protected String buildErrorString(PsiElement location) {
        final PsiBinaryExpression expression = (PsiBinaryExpression)location;
        final PsiExpression lhs = expression.getLOperand();
        final PsiJavaToken sign = expression.getOperationSign();
        final boolean value;
        if (lhs instanceof PsiMethodCallExpression) {
            value = createContainsExpressionValue(sign, false);
        } else {
            value = createContainsExpressionValue(sign, true);
        }
        if (value) {
            return InspectionGadgetsBundle.message(
                    "pointless.indexof.comparison.always.true.problem.descriptor");
        } else {
            return InspectionGadgetsBundle.message(
                    "pointless.indexof.comparison.always.false.problem.descriptor");
        }
    }

    static boolean createContainsExpressionValue(
            @NotNull PsiJavaToken sign,
            boolean flipped) {
        final IElementType tokenType = sign.getTokenType();
        if (tokenType.equals(JavaTokenType.EQEQ)) {
            return false;
        }
        if (tokenType.equals(JavaTokenType.NE)) {
            return true;
        }
        if (flipped) {
            if (tokenType.equals(JavaTokenType.GT) ||
                    tokenType.equals(JavaTokenType.GE)) {
                return false;
            }
        } else {
            if (tokenType.equals(JavaTokenType.LT) ||
                    tokenType.equals(JavaTokenType.LE)) {
                return false;
            }
        }
        return true;
    }

    public BaseInspectionVisitor buildVisitor() {
        return new PointlessIndexOfComparisonVisitor();
    }

    private static class PointlessIndexOfComparisonVisitor
            extends BaseInspectionVisitor {

        public void visitBinaryExpression(PsiBinaryExpression expression) {
            super.visitBinaryExpression(expression);
            final PsiExpression rhs = expression.getROperand();
            if (rhs == null) {
                return;
            }
            if (!ComparisonUtils.isComparison(expression)) {
                return;
            }
            final PsiExpression lhs = expression.getLOperand();
            if (lhs instanceof PsiMethodCallExpression) {
                final PsiJavaToken sign = expression.getOperationSign();
                if (isPointLess(lhs, sign, rhs, false)) {
                    registerError(expression);
                }
            } else if (rhs instanceof PsiMethodCallExpression) {
                final PsiJavaToken sign = expression.getOperationSign();
                if (isPointLess(rhs, sign, lhs, true)) {
                    registerError(expression);
                }
            }
        }

        private static boolean isPointLess(PsiExpression lhs, PsiJavaToken sign,
                                           PsiExpression rhs, boolean flipped) {
            final PsiManager manager = lhs.getManager();
            final PsiMethodCallExpression callExpression =
                    (PsiMethodCallExpression)lhs;
            if (!isIndexOfCall(callExpression)) {
                return false;
            }
            final PsiConstantEvaluationHelper constantEvaluationHelper =
                    manager.getConstantEvaluationHelper();
            final Object object =
                    constantEvaluationHelper.computeConstantExpression(rhs);
            if (!(object instanceof Integer)) {
                return false;
            }
            final Integer integer = (Integer)object;
            final int constant = integer.intValue();
            final IElementType tokenType = sign.getTokenType();
            if (tokenType == null) {
                return false;
            }
            if (flipped) {
                if (constant < 0 && (tokenType.equals(JavaTokenType.GT) ||
                        tokenType.equals(JavaTokenType.LE))) {
                    return true;
                } else if (constant < -1 &&
                        (tokenType.equals(JavaTokenType.GE) ||
                                tokenType.equals(JavaTokenType.LT) ||
                                tokenType.equals(JavaTokenType.NE) ||
                                tokenType.equals(JavaTokenType.EQEQ))) {
                    return true;
                }

            } else {
                if (constant < 0 && (tokenType.equals(JavaTokenType.LT) ||
                        tokenType.equals(JavaTokenType.GE))) {
                    return true;
                } else if (constant < -1 &&
                        (tokenType.equals(JavaTokenType.LE) ||
                                tokenType.equals(JavaTokenType.GT) ||
                                tokenType.equals(JavaTokenType.NE) ||
                                tokenType.equals(JavaTokenType.EQEQ))) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isIndexOfCall(
                @NotNull PsiMethodCallExpression expression) {
            final PsiReferenceExpression methodExpression =
                    expression.getMethodExpression();
            final String methodName = methodExpression.getReferenceName();
            return HardcodedMethodConstants.INDEX_OF.equals(methodName);
        }
    }
}