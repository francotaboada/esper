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

package com.espertech.esper.event.arr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.event.EventAdapterService;

/**
 * A getter that works on EventBean events residing within a Map as an event property.
 */
public class ObjectArrayNestedEntryPropertyGetterObjectArray extends ObjectArrayNestedEntryPropertyGetterBase {

    private final ObjectArrayEventPropertyGetter arrayGetter;

    public ObjectArrayNestedEntryPropertyGetterObjectArray(int propertyIndex, EventType fragmentType, EventAdapterService eventAdapterService, ObjectArrayEventPropertyGetter arrayGetter) {
        super(propertyIndex, fragmentType, eventAdapterService);
        this.arrayGetter = arrayGetter;
    }

    public Object handleNestedValue(Object value) {
        if (!(value instanceof Object[]))
        {
            if (value instanceof EventBean) {
                return arrayGetter.get((EventBean) value);
            }
            return null;
        }
        return arrayGetter.getObjectArray((Object[]) value);
    }

    public Object handleNestedValueFragment(Object value) {
        if (!(value instanceof Object[]))
        {
            if (value instanceof EventBean) {
                return arrayGetter.getFragment((EventBean) value);
            }
            return null;
        }

        // If the map does not contain the key, this is allowed and represented as null
        EventBean eventBean = eventAdapterService.adapterForTypedObjectArray((Object[]) value, fragmentType);
        return arrayGetter.getFragment(eventBean);
    }

    public boolean handleNestedValueExists(Object value) {
        if (!(value instanceof Object[]))
        {
            if (value instanceof EventBean) {
                return arrayGetter.isExistsProperty((EventBean) value);
            }
            return false;
        }
        return arrayGetter.isObjectArrayExistsProperty((Object[]) value);
    }
}
