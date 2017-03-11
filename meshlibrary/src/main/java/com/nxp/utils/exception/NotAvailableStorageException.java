package com.nxp.utils.exception;


public class NotAvailableStorageException
        extends Exception {
    private static final long serialVersionUID = 8520846818241106143L;


    public NotAvailableStorageException() {
    }


    public NotAvailableStorageException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NotAvailableStorageException(String detailMessage) {
        super(detailMessage);
    }

    public NotAvailableStorageException(Throwable throwable) {
        super(throwable);
    }
}

