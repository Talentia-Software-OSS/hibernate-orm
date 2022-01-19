/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * DB2 400 oblige à utiliser un cast pour les marques de parametre en dessous de la version v5r4m0. Sinon : [SQL0418] Utilisation incorrecte d'une marque de
 * paramètre.
 * 
 * @author Eric Bouladier
 */
public class LowerWithArgumentCastFunction extends StandardSQLFunction {
	private final TemplateRenderer renderer;

	public LowerWithArgumentCastFunction(String castType) {
		super("lower");
		renderer = new TemplateRenderer("lower(cast(?1 as " + castType + "))");
	}

	@Override
	public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		return renderer.render(args, factory);
	}
}
