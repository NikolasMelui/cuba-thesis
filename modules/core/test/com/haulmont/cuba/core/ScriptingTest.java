/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.security.entity.User;
import groovy.lang.Binding;

/**
 * @author krivopustov
 * @version $Id$
 */
public class ScriptingTest extends CubaTestCase {

    protected Scripting scripting;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        scripting = AppBeans.get(Scripting.class);
    }

    public void testSimpleEvaluate() throws Exception {
        Integer intResult = scripting.evaluateGroovy("2 + 2", new Binding());
        assertEquals((Integer)4, intResult);

        Binding binding = new Binding();
        binding.setVariable("instance", new User());
        Boolean boolResult = scripting.evaluateGroovy("return PersistenceHelper.isNew(instance)", binding);
        assertTrue(boolResult);
    }

    public void testImportsEvaluate() {
        String result = scripting.evaluateGroovy("import com.haulmont.bali.util.StringHelper\n" +
                                                 "return StringHelper.removeExtraSpaces(' Hello! ')", (Binding) null);
        assertNotNull(result);
    }

    public void testPackageAndImportsEvaluate() {
        String result = scripting.evaluateGroovy("package com.haulmont.cuba.core\n" +
                "import com.haulmont.bali.util.StringHelper\n" +
                "return StringHelper.removeExtraSpaces(' Hello! ')", (Binding) null);
        assertNotNull(result);
    }
    
    public void testPackageOnlyEvaluate() {
        Binding binding = new Binding();
        binding.setVariable("instance", new User());
        Boolean result = scripting.evaluateGroovy("package com.haulmont.cuba.core\n"+
                                       "return PersistenceHelper.isNew(instance)", binding);
        assertTrue(result);
    }
}
