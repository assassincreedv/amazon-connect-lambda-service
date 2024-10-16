package com.amazonaws.kvstranscribestreaming;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.transcribestreaming.FileByteToAudioEventSubscription;
import com.amazonaws.transcribestreaming.KVSByteToAudioEventSubscription;
import com.amazonaws.transcribestreaming.StreamTranscriptionBehaviorImpl;
import com.amazonaws.transcribestreaming.TranscribeStreamingRetryClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrate Amazon Connect's real-time transcription feature using AWS Kinesis Video Streams and AWS Transcribe.
 * The data flow is :
 * <p>
 * Amazon Connect => AWS KVS => AWS Transcribe => AWS DynamoDB
 *
 * <p>Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.</p>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class KVSTranscribeStreamingLambda implements RequestHandler<Object, String> {

    private final String hostUrl = "https://apiv3.hitalentech.com/voip";

    private static final Logger logger = LoggerFactory.getLogger(KVSTranscribeStreamingLambda.class);

    /**
     * Handler function for the Lambda
     *
     * @param object
     * @param context
     * @return
     */
    @Override
    public String handleRequest(Object object, Context context) {

        logger.info("received request : " + object.toString());
        logger.info("received context: " + context.toString());
        logger.info("get streaming info: " + object);

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(object);
            JsonNode rootNode = mapper.readTree(jsonString);
            String phone = rootNode.path("Details").path("ContactData").path("CustomerEndpoint").path("Address").asText();
            String contactId = rootNode.path("Details").path("ContactData").path("ContactId").asText();
            String streamARN = rootNode.path("Details").path("ContactData").path("MediaStreams").path("Customer").path("Audio").path("StreamARN").asText();
            String startFragmentNumber = rootNode.path("Details").path("ContactData").path("MediaStreams").path("Customer").path("Audio").path("StartFragmentNumber").asText();
            String accountARN =  rootNode.path("Details").path("Parameters").path("agentARN").asText();
            logger.info("accountARN: " + accountARN);
            ConnectTranscriptionRequest connectTranscriptionRequest = new ConnectTranscriptionRequest(phone, streamARN, startFragmentNumber,contactId, accountARN);
            // 创建HttpClient实例
            HttpClient client = HttpClients.createDefault();

            // 创建HttpPost实例
            HttpPost post = new HttpPost(hostUrl + "/api/v3/connect/live-transcription/aliCloud");
            post.setHeader("Content-Type", "application/json");
            String jsonBody = mapper.writeValueAsString(connectTranscriptionRequest);
            post.setEntity(new StringEntity(jsonBody));

            // 发送POST请求并获取响应
            HttpResponse response = client.execute(post);

            // 读取响应内容
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    /**
     * Starts streaming between KVS and Transcribe
     * The transcript segments are continuously saved to the Dynamo DB table
     * At end of the streaming session, the raw audio is saved as an s3 object
     *
     * @param streamARN
     * @param startFragmentNum
     * @param contactId
     * @param languageCode
     * @throws Exception
     */

    /**
     * Fetches the specified audio file from S3
     * The transcript segments are continuously saved to the Dynamo DB table
     *
     * @param inputFileName
     * @param languageCode the language code to be used for Transcription (optional; see https://docs.aws.amazon.com/transcribe/latest/dg/API_streaming_StartStreamTranscription.html#API_streaming_StartStreamTranscription_RequestParameters )
     * @throws Exception
     */


    /**
     * Closes the FileOutputStream and uploads the Raw audio file to S3
     *
     * @param kvsStreamTrackObject
     * @param saveCallRecording should the call recording be uploaded to S3?
     * @throws IOException
     */

}
