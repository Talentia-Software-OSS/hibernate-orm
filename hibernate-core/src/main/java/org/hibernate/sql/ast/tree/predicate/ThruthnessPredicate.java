/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Gavin King
 */
public class ThruthnessPredicate extends AbstractPredicate {
	private final Expression expression;
	private final boolean value;

	public ThruthnessPredicate(Expression expression, boolean value, boolean negated, JdbcMappingContainer expressionType) {
		super( expressionType, negated );
		this.expression = expression;
		this.value = value;
	}

	public boolean getBooleanValue() {
		return value;
	}

	public Expression getExpression() {
		return expression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitThruthnessPredicate( this );
	}
}
