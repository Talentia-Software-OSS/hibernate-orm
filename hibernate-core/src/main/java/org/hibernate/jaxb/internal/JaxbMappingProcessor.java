/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jaxb.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.jaxb.spi.orm.JaxbEntityMappings;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.XsdException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.engine.config.spi.StandardConverters;

/**
 * Loads {@code hbm.xml} and {@code orm.xml} files and processes them using StAX and JAXB.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class JaxbMappingProcessor {
	private static final Logger log = Logger.getLogger( JaxbMappingProcessor.class );

	public static final String ASSUMED_ORM_XSD_VERSION = "2.0";
	public static final String VALIDATE_XML_SETTING = "hibernate.xml.validate";
	public static final String HIBERNATE_MAPPING_URI = "http://www.hibernate.org/xsd/hibernate-mapping";

	private final ServiceRegistry serviceRegistry;
	private final boolean validateXml;

	public JaxbMappingProcessor(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, true );
//		this(
//				serviceRegistry,
//				serviceRegistry.getService( ConfigurationService.class ).getSetting(
//						VALIDATE_XML_SETTING,
//						StandardConverters.BOOLEAN,
//						true
//				)
//		);
	}

	public JaxbMappingProcessor(ServiceRegistry serviceRegistry, boolean validateXml) {
		this.serviceRegistry = serviceRegistry;
		this.validateXml = validateXml;
	}

	public JaxbRoot unmarshal(InputStream stream, Origin origin) {
		try {
			XMLEventReader staxReader = staxFactory().createXMLEventReader( stream );
			try {
				return unmarshal( staxReader, origin );
			}
			finally {
				try {
					staxReader.close();
				}
				catch ( Exception ignore ) {
				}
			}
		}
		catch ( XMLStreamException e ) {
			throw new MappingException( "Unable to create stax reader", e, origin );
		}
	}

	private XMLInputFactory staxFactory;

	private XMLInputFactory staxFactory() {
		if ( staxFactory == null ) {
			staxFactory = buildStaxFactory();
		}
		return staxFactory;
	}

	@SuppressWarnings( { "UnnecessaryLocalVariable" })
	private XMLInputFactory buildStaxFactory() {
		XMLInputFactory staxFactory = XMLInputFactory.newInstance();
		return staxFactory;
	}

	private static final QName ORM_VERSION_ATTRIBUTE_QNAME = new QName( "version" );

	@SuppressWarnings( { "unchecked" })
	private JaxbRoot unmarshal(XMLEventReader staxEventReader, final Origin origin) {
		XMLEvent event;
		try {
			event = staxEventReader.peek();
			while ( event != null && !event.isStartElement() ) {
				staxEventReader.nextEvent();
				event = staxEventReader.peek();
			}
		}
		catch ( Exception e ) {
			throw new MappingException( "Error accessing stax stream", e, origin );
		}

		if ( event == null ) {
			throw new MappingException( "Could not locate root element", origin );
		}

		final Schema validationSchema;
		final Class jaxbTarget;

		final String elementName = event.asStartElement().getName().getLocalPart();

		if ( "entity-mappings".equals( elementName ) ) {
			final Attribute attribute = event.asStartElement().getAttributeByName( ORM_VERSION_ATTRIBUTE_QNAME );
			final String explicitVersion = attribute == null ? null : attribute.getValue();
			validationSchema = validateXml ? resolveSupportedOrmXsd( explicitVersion ) : null;
			jaxbTarget = JaxbEntityMappings.class;
		}
		else {
			if ( !isNamespaced( event.asStartElement() ) ) {
				// if the elements are not namespaced, wrap the reader in a reader which will namespace them as pulled.
				log.debug( "HBM mapping document did not define namespaces; wrapping in custom event reader to introduce namespace information" );
				staxEventReader = new NamespaceAddingEventReader( staxEventReader, HIBERNATE_MAPPING_URI );
			}
			validationSchema = validateXml ? hbmSchema() : null;
			jaxbTarget = JaxbHibernateMapping.class;
		}

		final Object target;
		final ContextProvidingValidationEventHandler handler = new ContextProvidingValidationEventHandler();
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( jaxbTarget );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( validationSchema );
			unmarshaller.setEventHandler( handler );
			target = unmarshaller.unmarshal( staxEventReader );
		}
		catch ( JAXBException e ) {
			StringBuilder builder = new StringBuilder();
			builder.append( "Unable to perform unmarshalling at line number " );
			builder.append( handler.getLineNumber() );
			builder.append( " and column " );
			builder.append( handler.getColumnNumber() );
			builder.append( ". Message: " );
			builder.append( handler.getMessage() );
			throw new MappingException( builder.toString(), e, origin );
		}

		return new JaxbRoot( target, origin );
	}

	private boolean isNamespaced(StartElement startElement) {
		return ! "".equals( startElement.getName().getNamespaceURI() );
	}

	@SuppressWarnings( { "unchecked" })
	public JaxbRoot unmarshal(Document document, Origin origin) {
		Element rootElement = document.getDocumentElement();
		if ( rootElement == null ) {
			throw new MappingException( "No root element found", origin );
		}

		final Schema validationSchema;
		final Class jaxbTarget;

		if ( "entity-mappings".equals( rootElement.getNodeName() ) ) {
			final String explicitVersion = rootElement.getAttribute( "version" );
			validationSchema = validateXml ? resolveSupportedOrmXsd( explicitVersion ) : null;
			jaxbTarget = JaxbEntityMappings.class;
		}
		else {
			validationSchema = validateXml ? hbmSchema() : null;
			jaxbTarget = JaxbHibernateMapping.class;
		}

		final Object target;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance( jaxbTarget );
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshaller.setSchema( validationSchema );
			target = unmarshaller.unmarshal( new DOMSource( document ) );
		}
		catch ( JAXBException e ) {
			throw new MappingException( "Unable to perform unmarshalling", e, origin );
		}

		return new JaxbRoot( target, origin );
	}

	private Schema resolveSupportedOrmXsd(String explicitVersion) {
		final String xsdVersionString = explicitVersion == null ? ASSUMED_ORM_XSD_VERSION : explicitVersion;
		if ( "1.0".equals( xsdVersionString ) ) {
			return orm1Schema();
		}
		else if ( "2.0".equals( xsdVersionString ) ) {
			return orm2Schema();
		}
		throw new IllegalArgumentException( "Unsupported orm.xml XSD version encountered [" + xsdVersionString + "]" );
	}

	public static final String HBM_SCHEMA_NAME = "org/hibernate/hibernate-mapping-4.0.xsd";
	public static final String ORM_1_SCHEMA_NAME = "org/hibernate/ejb/orm_1_0.xsd";
	public static final String ORM_2_SCHEMA_NAME = "org/hibernate/ejb/orm_2_0.xsd";

	private Schema hbmSchema;

	private Schema hbmSchema() {
		if ( hbmSchema == null ) {
			hbmSchema = resolveLocalSchema( HBM_SCHEMA_NAME );
		}
		return hbmSchema;
	}

	private Schema orm1Schema;

	private Schema orm1Schema() {
		if ( orm1Schema == null ) {
			orm1Schema = resolveLocalSchema( ORM_1_SCHEMA_NAME );
		}
		return orm1Schema;
	}

	private Schema orm2Schema;

	private Schema orm2Schema() {
		if ( orm2Schema == null ) {
			orm2Schema = resolveLocalSchema( ORM_2_SCHEMA_NAME );
		}
		return orm2Schema;
	}

	private Schema resolveLocalSchema(String schemaName) {
		return resolveLocalSchema( schemaName, XMLConstants.W3C_XML_SCHEMA_NS_URI );
	}

	private Schema resolveLocalSchema(String schemaName, String schemaLanguage) {
		URL url = serviceRegistry.getService( ClassLoaderService.class ).locateResource( schemaName );
		if ( url == null ) {
			throw new XsdException( "Unable to locate schema [" + schemaName + "] via classpath", schemaName );
		}
		try {
			InputStream schemaStream = url.openStream();
			try {
				StreamSource source = new StreamSource( url.openStream() );
				SchemaFactory schemaFactory = SchemaFactory.newInstance( schemaLanguage );
				return schemaFactory.newSchema( source );
			}
			catch ( SAXException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			catch ( IOException e ) {
				throw new XsdException( "Unable to load schema [" + schemaName + "]", e, schemaName );
			}
			finally {
				try {
					schemaStream.close();
				}
				catch ( IOException e ) {
					log.debugf( "Problem closing schema stream [%s]", e.toString() );
				}
			}
		}
		catch ( IOException e ) {
			throw new XsdException( "Stream error handling schema url [" + url.toExternalForm() + "]", schemaName );
		}
	}

	static class ContextProvidingValidationEventHandler implements ValidationEventHandler {
		private int lineNumber;
		private int columnNumber;
		private String message;

		@Override
		public boolean handleEvent(ValidationEvent validationEvent) {
			ValidationEventLocator locator = validationEvent.getLocator();
			lineNumber = locator.getLineNumber();
			columnNumber = locator.getColumnNumber();
			message = validationEvent.getMessage();
			return false;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public int getColumnNumber() {
			return columnNumber;
		}

		public String getMessage() {
			return message;
		}
	}
}
