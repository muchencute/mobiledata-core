package com.muchencute.mobiledata.core;

import com.muchencute.commons.log.WebServiceLog;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Environment {

    private static Environment instance = null;

    private DataSource mDataSource;

    private Environment() {
        new WebServiceLog("mobiledata.core", "environment", "constructor", "")
                .setSuccess().println(Environment.class);
        try {
            initDataSource();
        } catch (NamingException e) {
            e.printStackTrace();
            new WebServiceLog("mobiledata.core", "environment", "constructor", "")
                    .setError(10, e.getLocalizedMessage())
                    .println(Environment.class);
        }
    }

    public static Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
    }

    private void initDataSource() throws NamingException {
        InitialContext initialContext = new InitialContext();
        mDataSource = (DataSource) initialContext.lookup(getJndiName());
    }

    private String getJndiName() {
        return "java:comp/env/jdbc/mobiledata.core";
    }

    public DataSource getDataSource() {
        return mDataSource;
    }
}
