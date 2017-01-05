package com.muchencute.mobiledata.core;

import com.muchencute.commons.database.Executed;
import com.muchencute.commons.database.ProcedureInvoker;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class InternalActions {

    private InternalActions() {

    }

    public static String getSecretKey(final String username) {

        final String[] secretKey = {null};

        ProcedureInvoker procedureInvoker = new ProcedureInvoker(Environment.getInstance().getDataSource());
        procedureInvoker.call("pro_GetSecretKey", username).executed(new Executed() {
            @Override
            public void executed(ResultSet resultSet, ArrayList<Object> arrayList) throws SQLException {
                if (resultSet.next()) {
                    secretKey[0] = resultSet.getString("account_secret");
                }
            }
        }).close();

        return secretKey[0];
    }

}
