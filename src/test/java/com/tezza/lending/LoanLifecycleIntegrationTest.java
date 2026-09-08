package com.tezza.lending;

import com.tezza.lending.customer.api.CustomerService;
import com.tezza.lending.customer.api.dto.CustomerRequest;
import com.tezza.lending.customer.api.dto.LoanLimitRequest;
import com.tezza.lending.loan.api.LoanService;
import com.tezza.lending.loan.api.dto.LoanRequest;
import com.tezza.lending.loan.api.dto.LoanResponse;
import com.tezza.lending.loan.api.dto.LoanSummaryResponse;
import com.tezza.lending.loan.internal.entity.enums.BillingCycleType;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanType;
import com.tezza.lending.product.api.ProductService;
import com.tezza.lending.product.api.dto.FeeRequest;
import com.tezza.lending.product.api.dto.ProductRequest;
import com.tezza.lending.product.api.dto.ProductResponse;
import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import com.tezza.lending.product.internal.entity.enums.TenureType;
import com.tezza.lending.repayment.api.RepaymentService;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"lending.notifications"})
class LoanLifecycleIntegrationTest {

    @BeforeAll
    static void checkDockerAvailable() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available — skipping integration tests. Start Docker and re-run.");
    }

    @Autowired LoanService loanService;
    @Autowired CustomerService customerService;
    @Autowired ProductService productService;
    @Autowired RepaymentService repaymentService;

    @Test
    void fullLoanLifecycle_disburseAndRepay_loanCloses() {
        // CREATE PRODUCT
        ProductRequest productReq = new ProductRequest();
        productReq.setName("Integration Test Loan " + System.currentTimeMillis());
        productReq.setDescription("Integration test product");
        productReq.setMinAmount(BigDecimal.valueOf(500));
        productReq.setMaxAmount(BigDecimal.valueOf(50000));
        productReq.setTenureValue(30);
        productReq.setTenureType(TenureType.DAYS);
        ProductResponse product = productService.createProduct(productReq);

        // CREATE CUSTOMER
        CustomerRequest customerReq = new CustomerRequest();
        customerReq.setFirstName("Test");
        customerReq.setLastName("User");
        customerReq.setEmail("integration-" + System.currentTimeMillis() + "@test.com");
        customerReq.setNationalId("IT-" + System.currentTimeMillis());
        var customer = customerService.createCustomer(customerReq);

        // SET LOAN LIMIT
        LoanLimitRequest limitReq = new LoanLimitRequest();
        limitReq.setMinLimit(BigDecimal.valueOf(500));
        limitReq.setMaxLimit(BigDecimal.valueOf(20000));
        limitReq.setCurrentLimit(BigDecimal.valueOf(10000));
        limitReq.setCreditScore(700);
        customerService.updateLoanLimit(customer.getId(), limitReq);

        // DISBURSE LOAN
        LoanRequest loanReq = new LoanRequest();
        loanReq.setCustomerId(customer.getId());
        loanReq.setProductId(product.getId());
        loanReq.setAmount(BigDecimal.valueOf(5000));
        loanReq.setLoanType(LoanType.LUMP_SUM);
        loanReq.setBillingCycleType(BillingCycleType.INDIVIDUAL);
        LoanResponse loan = loanService.disburseLoan(loanReq);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OPEN);
        assertThat(loan.getPrincipalAmount()).isEqualByComparingTo("5000");
        assertThat(loan.getCustomerId()).isEqualTo(customer.getId());

        // VERIFY LOAN APPEARS IN CUSTOMER LOAN LIST
        Page<LoanResponse> customerLoans = loanService.listLoans(null, customer.getId(), PageRequest.of(0, 10));
        assertThat(customerLoans.getTotalElements()).isEqualTo(1);

        // REPAY FULL AMOUNT
        RepaymentRequest repayReq = new RepaymentRequest();
        repayReq.setLoanId(loan.getId());
        repayReq.setAmount(loan.getOutstandingBalance());
        repayReq.setReference("IT-REF-" + System.currentTimeMillis());
        repayReq.setChannel(PaymentChannel.MPESA);
        repaymentService.processRepayment(repayReq);

        // LOAN SHOULD BE CLOSED
        LoanResponse closed = loanService.getLoan(loan.getId());
        assertThat(closed.getStatus()).isEqualTo(LoanStatus.CLOSED);
        assertThat(closed.getOutstandingBalance()).isEqualByComparingTo("0");
    }

    @Test
    void loanSummary_afterPartialRepayment_showsCorrectTotalPaid() {
        // SETUP
        ProductRequest productReq = new ProductRequest();
        productReq.setName("Summary Test Loan " + System.currentTimeMillis());
        productReq.setDescription("Summary test");
        productReq.setMinAmount(BigDecimal.valueOf(100));
        productReq.setMaxAmount(BigDecimal.valueOf(50000));
        productReq.setTenureValue(30);
        productReq.setTenureType(TenureType.DAYS);
        
        ProductResponse product = productService.createProduct(productReq);

        CustomerRequest customerReq = new CustomerRequest();
        customerReq.setFirstName("Summary");
        customerReq.setLastName("Test");
        customerReq.setEmail("summary-" + System.currentTimeMillis() + "@test.com");
        customerReq.setNationalId("SUM-" + System.currentTimeMillis());
        var customer = customerService.createCustomer(customerReq);

        LoanLimitRequest limitReq = new LoanLimitRequest();
        limitReq.setMinLimit(BigDecimal.valueOf(100));
        limitReq.setMaxLimit(BigDecimal.valueOf(20000));
        limitReq.setCurrentLimit(BigDecimal.valueOf(10000));
        limitReq.setCreditScore(700);
        customerService.updateLoanLimit(customer.getId(), limitReq);

        LoanRequest loanReq = new LoanRequest();
        loanReq.setCustomerId(customer.getId());
        loanReq.setProductId(product.getId());
        loanReq.setAmount(BigDecimal.valueOf(1000));
        loanReq.setLoanType(LoanType.LUMP_SUM);
        loanReq.setBillingCycleType(BillingCycleType.INDIVIDUAL);
        LoanResponse loan = loanService.disburseLoan(loanReq);

        // PARTIAL REPAYMENT
        RepaymentRequest repayReq = new RepaymentRequest();
        repayReq.setLoanId(loan.getId());
        repayReq.setAmount(BigDecimal.valueOf(400));
        repayReq.setReference("PARTIAL-" + System.currentTimeMillis());
        repayReq.setChannel(PaymentChannel.BANK);
        repaymentService.processRepayment(repayReq);

        // SUMMARY SHOWS CORRECT totalPaid FROM ACTUAL REPAYMENTS
        LoanSummaryResponse summary = loanService.getLoanSummary(loan.getId());
        assertThat(summary.getTotalPaid()).isEqualByComparingTo("400");
        assertThat(summary.getStatus()).isEqualTo(LoanStatus.OPEN);
    }

    @Test
    void disburseLoan_exceedsCustomerLimit_throws() {
        ProductRequest productReq = new ProductRequest();
        productReq.setName("Limit Test " + System.currentTimeMillis());
        productReq.setDescription("Limit test");
        productReq.setMinAmount(BigDecimal.valueOf(100));
        productReq.setMaxAmount(BigDecimal.valueOf(100000));
        productReq.setTenureValue(30);
        productReq.setTenureType(TenureType.DAYS);
        
        ProductResponse product = productService.createProduct(productReq);

        CustomerRequest customerReq = new CustomerRequest();
        customerReq.setFirstName("Limited");
        customerReq.setLastName("Customer");
        customerReq.setEmail("limited-" + System.currentTimeMillis() + "@test.com");
        customerReq.setNationalId("LIM-" + System.currentTimeMillis());
        var customer = customerService.createCustomer(customerReq);

        LoanLimitRequest limitReq = new LoanLimitRequest();
        limitReq.setMinLimit(BigDecimal.valueOf(100));
        limitReq.setMaxLimit(BigDecimal.valueOf(5000));
        limitReq.setCurrentLimit(BigDecimal.valueOf(1000));
        limitReq.setCreditScore(600);
        customerService.updateLoanLimit(customer.getId(), limitReq);

        LoanRequest loanReq = new LoanRequest();
        loanReq.setCustomerId(customer.getId());
        loanReq.setProductId(product.getId());
        loanReq.setAmount(BigDecimal.valueOf(9999));
        loanReq.setLoanType(LoanType.LUMP_SUM);
        loanReq.setBillingCycleType(BillingCycleType.INDIVIDUAL);

        assertThatThrownBy(() -> loanService.disburseLoan(loanReq))
                .hasMessageContaining("exceeds customer loan limit");
    }
}
