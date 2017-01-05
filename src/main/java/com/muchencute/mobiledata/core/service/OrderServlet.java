package com.muchencute.mobiledata.core.service;

import com.muchencute.commons.database.Executed;
import com.muchencute.commons.database.OutParam;
import com.muchencute.commons.database.ProcedureInvoker;
import com.muchencute.commons.encrypt.AESGenerator;
import com.muchencute.commons.protocol.JERObject;
import com.muchencute.commons.validator.Validator;
import com.muchencute.mobiledata.core.Environment;
import com.muchencute.mobiledata.core.InternalActions;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

public class OrderServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        ServletInputStream servletInputStream = req.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(servletInputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            stringBuilder.append(line);
        }

        JSONObject jsonObject = new JSONObject(stringBuilder.toString());

        final JERObject jerObject = new JERObject();

        String transId = jsonObject.optString("transId", "");
        String accountName = jsonObject.optString("accountName", "");
        String cellphone = jsonObject.optString("cellphone", "");
        int amount = jsonObject.optInt("amount", 0);
        String region = jsonObject.optString("region", "");
        String sign = jsonObject.optString("sign", "");

        Validator validator = new Validator();
        if (!validator.isUsername(accountName, "客户号不合法")
                .isCellphone(cellphone, "手机号不合法")
                .isTrue(amount > 0, "充值面额不合法")
                .isTrue("全国".equals(region) || "本地".equals(region), "区域不合法")
                .isNotNullOrEmptyAfterTrim(sign, "签名不能为空")
                .isNotNullOrEmptyAfterTrim(transId, "流水号不合法")
                .isPassed()) {
            jerObject.setError(11, validator.getErrorMessage());
            resp.getWriter().write(jerObject.toString());
            return;
        }

        String secretKey = InternalActions.getSecretKey(accountName);
        String rawData = AESGenerator.decrypt(sign, secretKey);
        JSONObject newJsonObject = new JSONObject(rawData);
        if (!(newJsonObject.optString("transId", "").equals(transId) &&
                newJsonObject.optString("accountName", "").equals(accountName) &&
                newJsonObject.optString("cellphone", "").equals(cellphone) &&
                newJsonObject.optInt("amount", 0) == amount &&
                newJsonObject.optString("region", "").equals(region))) {
            jerObject.setError(11, "签名不合法");
            resp.getWriter().write(jerObject.toString());
            return;
        }

        ProcedureInvoker procedureInvoker = new ProcedureInvoker(Environment.getInstance().getDataSource());
        procedureInvoker.call("pro_Core_order", accountName, cellphone, amount, region, new OutParam(Types.INTEGER)).executed(new Executed() {
            @Override
            public void executed(ResultSet resultSet, ArrayList<Object> arrayList) throws SQLException {
                int status = (int) arrayList.get(0);
                if (status == 0) {
                    jerObject.setSuccess();
                } else {
                    jerObject.setError(status, "订单失败");
                }
            }
        }).close();

        if (procedureInvoker.isErrorOccured()) {
            jerObject.setError(10, procedureInvoker.getErrorMessage());
        }

        resp.getWriter().write(jerObject.toString());
    }
}