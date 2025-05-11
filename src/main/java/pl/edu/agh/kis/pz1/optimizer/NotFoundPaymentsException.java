package pl.edu.agh.kis.pz1.optimizer;


/**
 * Exception thrown by the {@link PaymentOptimizer} when it cannot find
 * a suitable payment method to pay for a specific order, indicating
 * that a complete payment allocation is not possible under the given
 * constraints and algorithm strategy.
 */
public class NotFoundPaymentsException extends Exception {

    /**
     * Constructs a new NotFoundPaymentsException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link Throwable#initCause}.
     *
     * @param s the detail message. The detail message is saved for
     *          later retrieval by the {@link Throwable#getMessage()} method.
     */
    public NotFoundPaymentsException(String s) {
        super(s);
    }

}
