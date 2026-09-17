package com.coldchain.shared.response;

public record ResponseMeta(PaginationMeta pagination) {

    public static ResponseMeta empty() {
        return new ResponseMeta(null);
    }

    public static ResponseMeta paginated(PaginationMeta pagination) {
        return new ResponseMeta(pagination);
    }
}
