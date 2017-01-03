/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.event;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBeanSimple;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.util.JavaClassHelper;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.espertech.esper.supportregression.event.SupportEventInfra.*;

public class TestEventInfraUnderlyingAndSimpleProp extends TestCase {
    private final static String BEAN_TYPENAME = SupportBeanSimple.class.getSimpleName();

    private static final FunctionSendEventIntString FMAP = (epService, a, b) -> {
        Map<String, Object> map = new HashMap<>();
        map.put("myInt", a);
        map.put("myString", b);
        epService.getEPRuntime().sendEvent(map, MAP_TYPENAME);
        return map;
    };

    private static final FunctionSendEventIntString FOA = (epService, a, b) -> {
        Object[] oa = new Object[] {a, b};
        epService.getEPRuntime().sendEvent(oa, OA_TYPENAME);
        return oa;
    };

    private static final FunctionSendEventIntString FBEAN = (epService, a, b) -> {
        SupportBeanSimple bean = new SupportBeanSimple(b, a);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    };

    private static final FunctionSendEventIntString FXML = (epService, a, b) -> {
        String xml = "<myevent myInt=\"XXXXXX\" myString=\"YYYYYY\">\n" +
                "</myevent>\n";
        xml = xml.replace("XXXXXX", a.toString());
        xml = xml.replace("YYYYYY", b);
        try {
            Document d = SupportXML.sendEvent(epService.getEPRuntime(), xml);
            return d.getDocumentElement();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    private static final FunctionSendEventIntString FAVRO = (epService, a, b) -> {
        GenericData.Record datum = new GenericData.Record(getAvroSchema());
        datum.put("myInt", a);
        datum.put("myString", b);
        epService.getEPRuntime().sendEventAvro(datum, AVRO_TYPENAME);
        return datum;
    };

    private EPServiceProvider epService;

    protected void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        addMapEventType(configuration);
        addOAEventType(configuration);
        configuration.addEventType(BEAN_TYPENAME, SupportBeanSimple.class);
        addXMLEventType(configuration);
        addAvroEventType(configuration);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testPassUnderlyingGetViaPropertyExpression() {
        runAssertionPassUnderlying(MAP_TYPENAME, FMAP);
        runAssertionPassUnderlying(OA_TYPENAME, FOA);
        runAssertionPassUnderlying(BEAN_TYPENAME, FBEAN);
        runAssertionPassUnderlying(XML_TYPENAME, FXML);
        runAssertionPassUnderlying(AVRO_TYPENAME, FAVRO);
    }

    public void testPropertiesWGetter() {
        runAssertionPropertiesWGetter(MAP_TYPENAME, FMAP);
        runAssertionPropertiesWGetter(OA_TYPENAME, FOA);
        runAssertionPropertiesWGetter(BEAN_TYPENAME, FBEAN);
        runAssertionPropertiesWGetter(XML_TYPENAME, FXML);
        runAssertionPropertiesWGetter(AVRO_TYPENAME, FAVRO);
    }

    private void runAssertionPassUnderlying(String typename, FunctionSendEventIntString send) {
        String epl = "select * from " + typename;
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        String[] fields = "myInt,myString".split(",");

        assertEquals(Integer.class, JavaClassHelper.getBoxedType(statement.getEventType().getPropertyType("myInt")));
        assertEquals(String.class, statement.getEventType().getPropertyType("myString"));

        Object eventOne = send.apply(epService, 3, "some string");

        EventBean event = listener.assertOneGetNewAndReset();
        assertEquals(eventOne, event.getUnderlying());
        EPAssertionUtil.assertProps(event, fields, new Object[] {3, "some string"});

        Object eventTwo = send.apply(epService, 4, "other string");
        event = listener.assertOneGetNewAndReset();
        assertEquals(eventTwo, event.getUnderlying());
        EPAssertionUtil.assertProps(event, fields, new Object[] {4, "other string"});

        statement.destroy();
    }

    private void runAssertionPropertiesWGetter(String typename, FunctionSendEventIntString send) {
        String epl = "select myInt, exists(myInt) as exists_myInt, myString, exists(myString) as exists_myString from " + typename;
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);
        String[] fields = "myInt,exists_myInt,myString,exists_myString".split(",");

        assertEquals(Integer.class, JavaClassHelper.getBoxedType(statement.getEventType().getPropertyType("myInt")));
        assertEquals(String.class, statement.getEventType().getPropertyType("myString"));
        assertEquals(Boolean.class, statement.getEventType().getPropertyType("exists_myInt"));
        assertEquals(Boolean.class, statement.getEventType().getPropertyType("exists_myString"));

        send.apply(epService, 3, "some string");

        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[] {3, true, "some string", true});

        send.apply(epService, 4, "other string");
        event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[] {4, true, "other string", true});

        statement.destroy();
    }

    private void addMapEventType(Configuration configuration) {
        Properties properties = new Properties();
        properties.put("myInt", "int");
        properties.put("myString", "string");
        configuration.addEventType(MAP_TYPENAME, properties);
    }

    private void addOAEventType(Configuration configuration) {
        String[] names = {"myInt", "myString"};
        Object[] types = {Integer.class, String.class};
        configuration.addEventType(OA_TYPENAME, names, types);
    }

    private void addXMLEventType(Configuration configuration) {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "\t<xs:element name=\"myevent\">\n" +
                "\t\t<xs:complexType>\n" +
                "\t\t\t<xs:attribute name=\"myInt\" type=\"xs:int\" use=\"required\"/>\n" +
                "\t\t\t<xs:attribute name=\"myString\" type=\"xs:string\" use=\"required\"/>\n" +
                "\t\t</xs:complexType>\n" +
                "\t</xs:element>\n" +
                "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.addEventType(XML_TYPENAME, eventTypeMeta);
    }

    private void addAvroEventType(Configuration configuration) {
        configuration.addEventTypeAvro(AVRO_TYPENAME, new ConfigurationEventTypeAvro(getAvroSchema()));
    }

    private static Schema getAvroSchema() {
        return SchemaBuilder.record(AVRO_TYPENAME)
                .fields()
                .name("myInt").type().intType().noDefault()
                .name("myString").type().stringBuilder().prop("avro.java.string", "String").endString().noDefault()
                .endRecord();
    }

    @FunctionalInterface
    interface FunctionSendEventIntString {
        public Object apply (EPServiceProvider epService, Integer intValue, String stringValue);
    }
}
