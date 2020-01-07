package uk.me.eastmans.hadbexample;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

// DB�pimport
import=javax.sql.DataSource;
import=java.sql.Connection;
import=java.sql.Statement;
import=javax.naming.InitialContext;

/**
 * Created by markeastman on 11/08/2016.
 */
@WebServlet(value="/hadbexample", name="hadbexample-servlet")
public class HADBExampleServlet  extends GenericServlet {

    private static final String COUNT_VALUE = "countValue";

    // We now want to use a session value to see if they persist across HA redirects
    public void service(ServletRequest req, ServletResponse res)
            throws IOException, ServletException
    {
        //
        // �Z�b�V�������擾����
        //
        String message = System.getenv("HOSTNAME"); // This will be the pod name
        if (req instanceof HttpServletRequest)
        {
            HttpServletRequest httpReq = (HttpServletRequest) req;
            HttpSession session = httpReq.getSession();
            Integer count = incrementCount( session );
            message = "From session " + session.getId() + ", for the " + count + " time on pod " + message;
        }
        
        //
        // DB����
        //
        
        //�R�l�N�V�������擾����jndi
        String jndi = "java:jboss/PostgresDSnXA";
        
        InitialContext context = null;
        Connection connection = null;
        
        //�R�l�N�V�����擾����
        String result = null;
        try {
            context = new InitialContext();
            DataSource dataSource = (DataSource) context.lookup(jndi);
            
            connection = dataSource.getConnection();
            //�g�����U�N�V�����������x����DB�ɂ���ĈقȂ�̂Őݒ�𓝈�
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            result = connection.getMetaData().getDatabaseProductName();
            
            Statement stmt = (Statement) connection.createStatement();
            String testsql = "create table tomcat_address (id integer, name character varying(20));";
            stmt.execute(testsql);
            testsql = "commit;";
            stmt.execute(testsql);
            
        }
        finally {
            if (context != null) {
                try {
                        context.close();
                }
                catch (Exception e) {
                }
            }
            if (connection != null) {
                try {
                        connection.close();
                }
                catch (Exception e) {
                }
            }
        }
        
        // HTML�o��
        res.getWriter().println("<html>");
        res.getWriter().println("<h4>");
        res.getWriter().println(message);
        res.getWriter().println("�ڑ�����DB [" + result + "]");
        res.getWriter().println("</h4>");
        res.getWriter().println("</html>");
    }

    private Integer incrementCount(HttpSession session)
    {
        Integer count = (Integer)session.getAttribute(COUNT_VALUE);
        if (count == null)
            count = 1;
        else
            count = count + 1;
        session.setAttribute(COUNT_VALUE,count);
        return count;
    }

}
