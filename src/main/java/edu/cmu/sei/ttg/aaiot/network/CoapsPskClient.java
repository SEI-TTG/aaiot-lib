package edu.cmu.sei.ttg.aaiot.network;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

/**
 * Simple COAP client using DTLS and PSK. Sets up a proper DTLS connection to a CoapsPskServer.
 * Created by sebastianecheverria on 8/28/17.
 */
public class CoapsPskClient
{
    protected CoapClient coapClient;
    protected String serverName;
    protected int serverPort;
    protected String keyId;
    protected byte[] key;

    /**
     * Constructor.
     * @param serverName The IP or hostname of the server.
     * @param serverPort The port of the server.
     * @param keyId The id of the PSK to use.
     * @param key The raw bytes of the 128-bit PSK.
     */
    public CoapsPskClient(String serverName, int serverPort, String keyId, byte[] key)
    {
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.keyId = keyId;
        this.key = key;

        coapClient = new CoapClient();
        coapClient.setEndpoint(CoapsPskServer.setupDtlsEndpoint(0, keyId, key));
    }

    /**
     * Sends a COAPS request and returns a CBOR object with the response.
     * @param resource The name of the resource to access.
     * @param method The method to use: "post" or "get".
     * @param payload A CBOR object containing payload, if method is "post".
     * @return
     */
    public CBORObject sendRequest(String resource, String method, CBORObject payload)
    {
        String uri = "coaps://" + serverName + ":" + serverPort + "/" + resource;
        coapClient.setURI(uri);

        System.out.println("Sending request to server: " + uri);
        CoapResponse response = null;
        if(method.toLowerCase().equals("post"))
        {
            response = coapClient.post(payload.EncodeToBytes(), MediaTypeRegistry.APPLICATION_CBOR);
        }
        else if(method.toLowerCase().equals("get"))
        {
            response = coapClient.get();
        }
        else
        {
            throw new RuntimeException("Method '" + method + "' not supported.");
        }

        if(response == null)
        {
            System.out.println("Server did not respond, timed out, or cancelled connection.");
            return null;
        }

        System.out.println("Response: " + Utils.prettyPrint(response));

        if(response.getCode() != CoAP.ResponseCode.CREATED &&
                response.getCode() != CoAP.ResponseCode.VALID &&
                response.getCode() != CoAP.ResponseCode.DELETED &&
                response.getCode() != CoAP.ResponseCode.CHANGED &&
                response.getCode() != CoAP.ResponseCode.CONTENT)
        {
            System.out.println("Error received in response: " + response.getCode());
            return null;
        }

        // We assume by now things went well.
        CBORObject responseData = null;
        try
        {
            responseData = CBORObject.DecodeFromBytes(response.getPayload());
            System.out.println("Response CBOR Payload: " + responseData);
        }
        catch(Exception e)
        {
            System.out.println("Reply was not CBOR.");
        }

        return responseData;
    }

    /**
     * Stops the internal DTLS endpoint.
     */
    public void stop()
    {
        if(coapClient != null)
        {
            coapClient.getEndpoint().stop();
        }
    }

}