/***** Copyright (c) 1999 Object Management Group. Unlimited rights to 
       duplicate and use this code are hereby granted provided that this 
       copyright notice is included.
*****/

package org.omg.CORBA;

public final class NO_PERMISSION extends org.omg.CORBA.SystemException {

    public NO_PERMISSION() {
        super("", 0, CompletionStatus.COMPLETED_NO);
    }

    public NO_PERMISSION(int minor, CompletionStatus completed) {
        super("", minor, completed);
    }

    public NO_PERMISSION(String reason) {
        super(reason, 0, CompletionStatus.COMPLETED_NO);
    }

    public NO_PERMISSION(String reason, int minor, CompletionStatus completed) {
        super(reason, minor, completed);
    }

}
