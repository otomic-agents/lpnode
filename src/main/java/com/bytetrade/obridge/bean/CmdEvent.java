package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import com.bytetrade.obridge.bean.PreBusiness;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CmdEvent {
    
    public static final String CMD_UPDATE_QUOTE = "CMD_UPDATE_QUOTE";
    public static final String EVENT_QUOTE_REMOVER = "EVENT_QUOTE_REMOVER";

    public static final String CMD_ASK_QUOTE = "CMD_ASK_QUOTE";
    public static final String EVENT_ASK_REPLY = "EVENT_ASK_REPLY";

    public static final String EVENT_LOCK_QUOTE = "EVENT_LOCK_QUOTE";
    public static final String CALLBACK_LOCK_QUOTE = "CALLBACK_LOCK_QUOTE";

    public static final String EVENT_TRANSFER_OUT = "EVENT_TRANSFER_OUT";
    public static final String CMD_TRANSFER_IN = "CMD_TRANSFER_IN";

    public static final String EVENT_TRANSFER_OUT_CONFIRM = "EVENT_TRANSFER_OUT_CONFIRM";
    public static final String CMD_TRANSFER_IN_CONFIRM = "CMD_TRANSFER_IN_CONFIRM";

    public static final String EVENT_TRANSFER_OUT_REFUND = "EVENT_TRANSFER_OUT_REFUND";
    public static final String CMD_TRANSFER_IN_REFUND = "CMD_TRANSFER_IN_REFUND";

    public static final String EVENT_TRANSFER_IN = "EVENT_TRANSFER_IN";
    public static final String EVENT_TRANSFER_IN_CONFIRM = "EVENT_TRANSFER_IN_CONFIRM";
    public static final String EVENT_TRANSFER_IN_REFUND = "EVENT_TRANSFER_IN_REFUND";



    String cmd;//update quote, update_business_transfer_out_callback(必须), lock_quote_callback(必须), unlock_quote_callback(非必要), quote_removed_callback(非必要)

    QuoteData quoteData;

    QuoteRemoveInfo quoteRemoveInfo;

    PreBusiness preBusiness;

    BusinessFullData businessFullData;

    String cid;

    String amount;
}
