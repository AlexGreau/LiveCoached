package com.example.livecoached.Service;

public interface Decoder {
    void decodeResponse(String rep);
    void errorMessage(String err);
}
