package io.mosip.registration.processor.data.verification.service.impl;

import static io.mosip.registration.processor.data.verification.constants.DataVerificationConstants.DATETIME_PATTERN;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import io.mosip.registration.processor.core.exception.DataShareException;
import io.mosip.registration.processor.data.verification.dto.*;
import io.mosip.registration.processor.data.verification.entity.DataVerificationEntity;
import io.mosip.registration.processor.data.verification.repository.DataVerificationRepository;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.core.cbeffutil.spi.CbeffUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.util.JsonUtils;
import io.mosip.registration.processor.core.abstractverticle.MessageBusAddress;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.code.EventId;
import io.mosip.registration.processor.core.code.EventName;
import io.mosip.registration.processor.core.code.EventType;
import io.mosip.registration.processor.core.code.ModuleName;
import io.mosip.registration.processor.core.code.RegistrationExceptionTypeCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionStatusCode;
import io.mosip.registration.processor.core.code.RegistrationTransactionTypeCode;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.constant.MappingJsonConstants;
import io.mosip.registration.processor.core.constant.PolicyConstant;
import io.mosip.registration.processor.core.constant.ProviderStageName;
import io.mosip.registration.processor.core.constant.RegistrationType;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.exception.PacketManagerException;
import io.mosip.registration.processor.core.exception.util.PlatformErrorMessages;
import io.mosip.registration.processor.core.exception.util.PlatformSuccessMessages;
import io.mosip.registration.processor.core.http.ResponseWrapper;
import io.mosip.registration.processor.core.kernel.master.dto.UserResponseDTOWrapper;
import io.mosip.registration.processor.core.logger.LogDescription;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.queue.MosipQueueManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.core.status.util.StatusUtil;
import io.mosip.registration.processor.core.status.util.TrimExceptionMessage;
import io.mosip.registration.processor.core.util.JsonUtil;
import io.mosip.registration.processor.core.util.RegistrationExceptionMapperUtil;
import io.mosip.registration.processor.data.verification.service.DataVerificationService;
import io.mosip.registration.processor.data.verification.stage.DataVerificationStage;
import io.mosip.registration.processor.packet.manager.exception.FileNotFoundInDestinationException;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;
import io.mosip.registration.processor.packet.storage.dto.Document;
import io.mosip.registration.processor.packet.storage.utils.BIRConverter;
import io.mosip.registration.processor.packet.storage.utils.PriorityBasedPacketManagerService;
import io.mosip.registration.processor.packet.storage.utils.Utilities;
import io.mosip.registration.processor.rest.client.audit.builder.AuditLogRequestBuilder;
import io.mosip.registration.processor.status.code.RegistrationStatusCode;
import io.mosip.registration.processor.status.dto.InternalRegistrationStatusDto;
import io.mosip.registration.processor.status.dto.RegistrationStatusDto;
import io.mosip.registration.processor.status.exception.TablenotAccessibleException;
import io.mosip.registration.processor.status.service.RegistrationStatusService;

import io.mosip.registration.processor.data.verification.constants.DataVerificationConstants;
import springfox.documentation.spring.web.json.Json;

/**
 * The Class DataVerificationServiceImpl.
 */
@Component
@Transactional
public class DataVerificationServiceImpl implements DataVerificationService {

	/** The logger. */
	private static final Logger regProcLogger = RegProcessorLogger.getLogger(DataVerificationServiceImpl.class);
	@Value("${registration.processor.queue.dataverification.request:mosip-to-dv}")
	private String mvRequestAddress;

	//FIXME
	@Value("${registration.processor.queue.manualverification.request.messageTTL}")
	private int mvRequestMessageTTL;
	private LinkedHashMap<String, Object> policies = null;

	@Autowired
	private Environment env;

	/** The address. */

	@Autowired
	private RegistrationProcessorRestClientService registrationProcessorRestClientService;

	@Autowired
	private CbeffUtil cbeffutil;

	@Autowired
	private Utilities utility;

	@Autowired
	private MosipQueueManager<MosipQueue, byte[]> mosipQueueManager;
	
	@Autowired
	private PriorityBasedPacketManagerService packetManagerService;

	/** The utilities. */
	@Autowired
	private Utilities utilities;

	/** The audit log request builder. */
	@Autowired
	private AuditLogRequestBuilder auditLogRequestBuilder;

	/** The registration status service. */
	@Autowired
	private RegistrationStatusService<String, InternalRegistrationStatusDto, RegistrationStatusDto> registrationStatusService;
	/** The base packet repository. */
	@Autowired
	private DataVerificationStage dataVerificationStage;

	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	RegistrationExceptionMapperUtil registrationExceptionMapperUtil;

	@Value("${registration.processor.data.verification.policy.id:mpolicy-default-dataverification}")
	private String policyId;

	@Value("${registration.processor.data.verification.subscriber.id:mpartner-default-dataverification}")
	private String subscriberId;

	@Value("${registration.processor.data.verification.filter:10001,10002}")
	private String filter;

	@Value("${registration.processor.data.verification.dv-policy-json: registration-processor-dv-policy.json}")
	private String dvPolicyJson;
	@Value("${config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	@Value("${activemq.message.format}")
	private String messageFormat;

	private static final String DATA_VERIFICATION = "dataverification";

	/** The Constant USER. */
	private static final String USER = "MOSIP_SYSTEM";
	private static final String TEXT_MESSAGE = "text";
	private static final String DATASHARE = "dataShare";
	private static final String ERRORS = "errors";
	private static final String URL = "url";
	private static final String META_INFO = "metaInfo";
	private static final String AUDITS = "audits";
	private JSONObject mappingJsonObject = null;
	@Autowired
	private ObjectMapper objMapper;
	@Autowired
	private DataVerificationRepository dataVerificationRepository;

	private DataVerificationPolicyDTO getSamplePolicy() throws IOException, JSONException {
		DataVerificationPolicyDTO policy = new DataVerificationPolicyDTO();


		if (mappingJsonObject == null) {
			String mappingJsonString = Utilities.getJson(configServerFileStorageURL, dvPolicyJson);
			mappingJsonObject = objMapper.readValue(mappingJsonString, JSONObject.class);

			System.out.printf("mapping json object:::"+mappingJsonObject.toJSONString());
		}

		JSONObject dvPolicyObject =  JsonUtil.getJSONObject(mappingJsonObject, "policy");
		System.out.printf("dvPolicyObject:::"+dvPolicyObject);


		org.json.simple.JSONArray center =  JsonUtil.getJSONArray(dvPolicyObject, "center");

		System.out.printf("center  policy::::"+center);
		List <String> center_list =  new ArrayList<>();;
		for (int i = 0; i < center.size(); i++) {
            center_list.add((String) center.get(i));
        }
		policy.setCenter(center_list);

		return policy;
	}
    /**
	 *  FIXME: simple filter/apply method
	 */
	public Boolean applySamplePolicy(String rid) throws IOException, JSONException {

		if(rid != null){
			String center_id = rid.substring(0, 5);

			System.out.printf("center: "+center_id);

			DataVerificationPolicyDTO policy = getSamplePolicy() ;

			List<String>  center_list = policy.getCenter();


			System.out.printf("center list from json policy:::"+center_list.toString());

			//String[] filter_list = filter.split(",");

			for (String c : center_list) {
				System.out.printf("c: \n"+c);
				if(c.equalsIgnoreCase(center_id)){
					return true;
				}
			}
		}
		return false;
	}
	@Override
	public MessageDTO process(MessageDTO object, MosipQueue queue) {
		InternalRegistrationStatusDto registrationStatusDto=new InternalRegistrationStatusDto();
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		LogDescription description = new LogDescription();
		String moduleName = ModuleName.DATA_VERIFICATION.toString();
		String moduleId = PlatformSuccessMessages.RPR_DATA_VERIFICATION_SENT.getCode();
		boolean isTransactionSuccessful = false;
		String registrationId = object.getRid();

		try {
			object.setInternalError(false);
			object.setIsValid(false);
			object.setMessageBusAddress(MessageBusAddress.DATA_VERIFICATION_BUS_IN);

			List<DataVerificationEntity> d = dataVerificationRepository.findAllByRegId(registrationId);

			if (null == object.getRid() || object.getRid().isEmpty())
				throw new Exception("Technify: Error");
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					object.getRid(), "DataVerificationServiceImpl::process()::entry");

			Boolean needToVerify = applySamplePolicy(registrationId);
			System.out.printf("Need to Verify::::"+needToVerify);
			String requestId = UUID.randomUUID().toString();

			registrationStatusDto = registrationStatusService
					.getRegistrationStatus(object.getRid());
			if(needToVerify){
				if(d.isEmpty()){
					// Insert entry
					saveDataVerificationData(registrationId, requestId);
					pushRequestToQueue(object.getRid(), requestId, queue);
					registrationStatusDto.setStatusComment(StatusUtil.RPR_DATA_VERIFICATION_SENT_TO_QUEUE.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.RPR_DATA_VERIFICATION_SENT_TO_QUEUE.getCode());
					registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
					registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());

				}else  if(!d.get(0).getStatusCode().equalsIgnoreCase("APPROVED") || !d.get(0).getStatusCode().equalsIgnoreCase("REJECTED")){
					// Insert entry
					// Send to DV service via activeMQ.
					/*
					 * FIXME: simple  filter logic to be used just for testing the stage
					 *
					 */
					pushRequestToQueue(object.getRid(), requestId, queue);
					registrationStatusDto.setStatusComment(StatusUtil.RPR_DATA_VERIFICATION_SENT_TO_QUEUE.getMessage());
					registrationStatusDto.setSubStatusCode(StatusUtil.RPR_DATA_VERIFICATION_SENT_TO_QUEUE.getCode());
					registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
					registrationStatusDto
							.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());
				}
				else {
					isTransactionSuccessful  = d.get(0).getStatusCode().equalsIgnoreCase("APPROVED")? true: false;
					object.setIsValid(isTransactionSuccessful);
					object.setMessageBusAddress(MessageBusAddress.DATA_VERIFICATION_BUS_OUT);
					dataVerificationStage.sendMessage(object);
				}
			} else {
				object.setIsValid(true);
				object.setMessageBusAddress(MessageBusAddress.DATA_VERIFICATION_BUS_OUT);
				dataVerificationStage.sendMessage(object);
			}

		} catch (DataShareException de) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.RPR_DATA_VERIFICATION_RESEND.getMessage() + de.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.RPR_DATA_VERIFICATION_RESEND.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			description.setCode(PlatformErrorMessages.RPR_DATA_VERIFICATION_RESEND.getCode());
			description.setMessage(PlatformErrorMessages.RPR_DATA_VERIFICATION_RESEND.getMessage());
			object.setInternalError(true);
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					de.getErrorCode(), de.getErrorText());

		}   catch (Exception e) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.FAILED.name());
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.RPR_DATA_VERIFICATION_RESEND.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.RPR_DATA_VERIFICATION_RESEND.getCode());
			registrationStatusDto.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.REPROCESS.toString());
			description.setCode(PlatformErrorMessages.RPR_DATA_VERIFICATION_RESEND.getCode());
			description.setMessage(PlatformErrorMessages.RPR_DATA_VERIFICATION_RESEND.getMessage());
			object.setInternalError(true);
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					e.getMessage(), e.getMessage());
		}finally {
			registrationStatusDto
					.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.DATA_VERIFICATION.toString());
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);
			if (object.getIsValid() && !object.getInternalError())
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "DataVerificationServiceImpl::process()::success");
			else
				regProcLogger.info(LoggerFileConstant.SESSIONID.toString(),
						LoggerFileConstant.REGISTRATIONID.toString(), registrationId, "DataVerificationServiceImpl::process()::failure");

			String eventId = isTransactionSuccessful ? EventId.RPR_402.toString() : EventId.RPR_405.toString();
			String eventName = isTransactionSuccessful ? EventName.UPDATE.toString() : EventName.EXCEPTION.toString();
			String eventType = isTransactionSuccessful ? EventType.BUSINESS.toString() : EventType.SYSTEM.toString();

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				object.getRid(), "DataVerificationServiceImpl::process()::entry");

		return object;
	}

	private void pushRequestToQueue(String refId, String requestId, MosipQueue queue) throws Exception {
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				refId, "DataVerificationServiceImpl::pushRequestToQueue()::entry");




		DataVerificationRequestDTO mar = prepareDataVerificationRequest(refId, requestId);


		System.out.println("Push: Request : " + JsonUtils.javaObjectToJsonString(mar));

		if (messageFormat.equalsIgnoreCase(TEXT_MESSAGE))
			mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar), mvRequestAddress, mvRequestMessageTTL);
		else
			mosipQueueManager.send(queue, JsonUtils.javaObjectToJsonString(mar).getBytes(), mvRequestAddress, mvRequestMessageTTL);
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				refId, "DataVerificationServiceImpl::pushRequestToQueue()::entry");

	}
	private DataVerificationRequestDTO prepareDataVerificationRequest(String rid, String requestId ) throws Exception {
		DataVerificationRequestDTO req = new DataVerificationRequestDTO();

		req.setId(DataVerificationConstants.DATA_VERIFICATION_ID);
		req.setVersion(DataVerificationConstants.VERSION);
		req.setRequestId(requestId);
		req.setRequesttime(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)));
		req.setReferenceId(rid);
		InternalRegistrationStatusDto registrationStatusDto = null;
		registrationStatusDto = registrationStatusService.getRegistrationStatus(rid);


		try {
			req.setReferenceURL(
					getDataShareUrl(rid, registrationStatusDto.getRegistrationType()));



		} catch (Exception ex) {
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					"Technify prepare shareable data error");
			throw ex;
		}

		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(), "",
				"DataVerificationServiceImpl::formVerificationRequest()::entry");


		return req;
	}

	private LinkedHashMap<String, Object> getPolicy() throws Exception {
		if (policies != null && policies.size() > 0)
			return policies;

		ResponseWrapper<?> policyResponse = (ResponseWrapper<?>) registrationProcessorRestClientService.getApi(
				ApiName.PMS, Lists.newArrayList("mpolicy-default-dataverification", PolicyConstant.PARTNER_ID, "mpartner-default-dataverification"), "", "", ResponseWrapper.class);


		if (policyResponse == null || (policyResponse.getErrors() != null && policyResponse.getErrors().size() >0)) {
			throw new Exception(policyResponse == null ? "Policy Response response is null" : policyResponse.getErrors().get(0).getMessage());

		} else {
			LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) policyResponse.getResponse();
			policies = (LinkedHashMap<String, Object>) responseMap.get(DataVerificationConstants.POLICIES);
		}
		return policies;

	}
	private Map<String, String> getPolicyMap(LinkedHashMap<String, Object> policies) throws Exception {
		Map<String, String> policyMap = new HashMap<>();
		List<LinkedHashMap> attributes = (List<LinkedHashMap>) policies.get(DataVerificationConstants.SHAREABLE_ATTRIBUTES);
		ObjectMapper mapper = new ObjectMapper();
		for (LinkedHashMap map : attributes) {
			ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(map),
					ShareableAttributes.class);
			policyMap.put(shareableAttributes.getAttributeName(), shareableAttributes.getSource().iterator().next().getAttribute());
		}

		for (Map.Entry<String, String> entry : policyMap.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			System.out.println(key + ": " + value);
		}
		return policyMap;

	}
	private String getDataShareUrl(String id, String process) throws Exception {


		DataShareRequestDto requestDto = new DataShareRequestDto();
		LinkedHashMap<String, Object> policy = getPolicy();
		Map<String, String> policyMap = getPolicyMap(policy);


		// set demographic
		Map<String, String> demographicMap = policyMap.entrySet().stream().filter(e-> e.getValue() != null &&
						(!META_INFO.equalsIgnoreCase(e.getValue()) && !AUDITS.equalsIgnoreCase(e.getValue())))
				.collect(Collectors.toMap(e-> e.getKey(),e -> e.getValue()));
		requestDto.setIdentity(packetManagerService.getFields(id, demographicMap.values().stream().collect(Collectors.toList()), process, ProviderStageName.DATA_VERIFICATION));


		Map<String, String> identity = packetManagerService.getFields(id, demographicMap.values().stream().collect(Collectors.toList()), process, ProviderStageName.DATA_VERIFICATION);


		// set documents
		JSONObject docJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.DOCUMENT);

		System.out.println("docJson"+docJson.toJSONString());
		for (Object doc : docJson.keySet()) {
			if (doc != null) {
				HashMap docmap = (HashMap) docJson.get(doc.toString());
				String docName = docmap != null && docmap.get(MappingJsonConstants.VALUE)!= null ? docmap.get(MappingJsonConstants.VALUE).toString() : null;
				System.out.println("docName: "+docName);

				if (policyMap.containsValue(docName)) {


					Document document = packetManagerService.getDocument(id, docName, process, ProviderStageName.DATA_VERIFICATION);

					if (document != null) {
						if (requestDto.getDocuments() != null)
							requestDto.getDocuments().put(docmap.get(MappingJsonConstants.VALUE).toString(), CryptoUtil.encodeBase64String(document.getDocument()));
						else {
							Map<String, String> docMap = new HashMap<>();
							docMap.put(docmap.get(MappingJsonConstants.VALUE).toString(), CryptoUtil.encodeBase64String(document.getDocument()));
							requestDto.setDocuments(docMap);
						}
					}
				}
			}
		}

		// set metainfo
		if (policyMap.containsValue(META_INFO)) {
			requestDto.setMetaInfo(JsonUtils.javaObjectToJsonString(packetManagerService.getMetaInfo(id, process, ProviderStageName.DATA_VERIFICATION)));
			System.out.println("Meta info::::");
		}
		// set biometrics
		JSONObject regProcessorIdentityJson = utility.getRegistrationProcessorMappingJson(MappingJsonConstants.IDENTITY);
		String individualBiometricsLabel = JsonUtil.getJSONValue(
				JsonUtil.getJSONObject(regProcessorIdentityJson, MappingJsonConstants.INDIVIDUAL_BIOMETRICS),
				MappingJsonConstants.VALUE);

		if (policyMap.containsValue(individualBiometricsLabel)) {
			List<String> modalities = getModalities(policy);
			BiometricRecord biometricRecord = packetManagerService.getBiometrics(
					id, individualBiometricsLabel, modalities, process, ProviderStageName.DATA_VERIFICATION);
			byte[] content = cbeffutil.createXML(BIRConverter.convertSegmentsToBIRList(biometricRecord.getSegments()));
			requestDto.setBiometrics(content != null ? CryptoUtil.encodeBase64(content) : null);
		}

		String req = JsonUtils.javaObjectToJsonString(requestDto);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("name", DATA_VERIFICATION);
		map.add("filename", DATA_VERIFICATION);

		ByteArrayResource contentsAsResource = new ByteArrayResource(req.getBytes()) {
			@Override
			public String getFilename() {
				return DATA_VERIFICATION;
			}
		};
		map.add("file", contentsAsResource);

		List<String> pathSegments = new ArrayList<>();
		pathSegments.add("mpolicy-default-dataverification");
		pathSegments.add("mpartner-default-dataverification");
		io.mosip.kernel.core.http.ResponseWrapper<DataShareResponseDto> resp = new io.mosip.kernel.core.http.ResponseWrapper<>();

		LinkedHashMap response = (LinkedHashMap) registrationProcessorRestClientService.postApi(ApiName.DATASHARECREATEURL, MediaType.MULTIPART_FORM_DATA, pathSegments, null, null, map, LinkedHashMap.class);
		if (response == null || (response.get(ERRORS) != null))
			throw new Exception(response == null ? "Datashare response is null" : response.get(ERRORS).toString());

		LinkedHashMap datashare = (LinkedHashMap) response.get(DATASHARE);


		return datashare.get(URL) != null ? datashare.get(URL).toString() : null;
	}

	public List<String> getModalities(LinkedHashMap<String, Object> policy) throws IOException{
		Map<String, List<String>> typeAndSubTypeMap = new HashMap<>();
		List<LinkedHashMap> attributes = (List<LinkedHashMap>) policy.get(DataVerificationConstants.SHAREABLE_ATTRIBUTES);
		ObjectMapper mapper = new ObjectMapper();
		for (LinkedHashMap map : attributes) {
			ShareableAttributes shareableAttributes = mapper.readValue(mapper.writeValueAsString(map),
					ShareableAttributes.class);
			for (Source source : shareableAttributes.getSource()) {
				List<Filter> filterList = source.getFilter();
				if (filterList != null && !filterList.isEmpty()) {
					filterList.forEach(filter -> {
						if (filter.getSubType() != null && !filter.getSubType().isEmpty()) {
							typeAndSubTypeMap.put(filter.getType(), filter.getSubType());
						} else {
							typeAndSubTypeMap.put(filter.getType(), null);
						}
					});
				}
			}
		}
		List<String> modalities=new ArrayList<>();
		for(Map.Entry<String, List<String>> entry : typeAndSubTypeMap.entrySet()) {
			if(entry.getValue() == null) {
				modalities.add(entry.getKey());
			} else {
				modalities.addAll(entry.getValue());
			}
		}
		return modalities;

	}
	@Override
	public boolean updatePacketStatus(DataVerificationResponseDTO resp, String stageName, MosipQueue queue){
		TrimExceptionMessage trimExceptionMessage = new TrimExceptionMessage();
		LogDescription description = new LogDescription();
		boolean isTransactionSuccessful = false;
		String regId = "";
		try {
			regId = validateRequestIdAndReturnRid(resp.getReferenceId());
			System.out.printf("REGID: :::"+regId);
		}
		catch (Exception e){
			System.out.printf(e.getMessage());
		}
		MessageDTO messageDTO = new MessageDTO();
		messageDTO.setInternalError(false);
		messageDTO.setIsValid(false);
		messageDTO.setRid(regId);

		if(regId == null){
			regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, "REG ID is null. ::This should not happen ...DataVerificationServiceImpl::updatePacketStatus()::Error");
			return false;
		}
		//
		messageDTO.setRid(regId);

		InternalRegistrationStatusDto registrationStatusDto = registrationStatusService
				.getRegistrationStatus(regId);
		registrationStatusDto.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.DATA_VERIFICATION.name());
		registrationStatusDto.setRegistrationStageName(stageName);
		//
		messageDTO.setReg_type(RegistrationType.valueOf(registrationStatusDto.getRegistrationType()));

		try {
			if (!regId.isEmpty()){
					DataVerificationEntity dve = dataVerificationRepository.getDataVerificationEntityForRID(regId);

				isTransactionSuccessful = successFlow(regId, resp, dve, registrationStatusDto, messageDTO, description);

//				dve.setReasonCode(resp.getReason());
//					dve.setStatusComment(resp.getComment());
//					dve.setStatusCode(resp.getDecision());
//					dataVerificationRepository.save(dve);
			}

			/*if(resp.getDecision().equalsIgnoreCase("APPROVED")){
				messageDTO.setIsValid(true);
				messageDTO.setMessageBusAddress(MessageBusAddress.DATA_VERIFICATION_BUS_OUT);
				dataVerificationStage.sendMessage(messageDTO);
				isTransactionSuccessful = true;

			} else {
				messageDTO.setIsValid(false);
				messageDTO.setMessageBusAddress(MessageBusAddress.DATA_VERIFICATION_BUS_OUT);
				dataVerificationStage.sendMessage(messageDTO);
			}
			*/
		} catch (TablenotAccessibleException e) {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.TABLE_NOT_ACCESSIBLE_EXCEPTION));
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.DB_NOT_ACCESSIBLE.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.DB_NOT_ACCESSIBLE.getCode());

			description.setMessage(PlatformErrorMessages.RPR_TABLE_NOT_ACCESSIBLE.getMessage());
			description.setCode(PlatformErrorMessages.RPR_TABLE_NOT_ACCESSIBLE.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (IOException e) {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.IOEXCEPTION));
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.IO_EXCEPTION.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.IO_EXCEPTION.getCode());

			description.setMessage(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.RPR_SYS_IO_EXCEPTION.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (Exception e) {
			registrationStatusDto.setLatestTransactionStatusCode(registrationExceptionMapperUtil
					.getStatusCode(RegistrationExceptionTypeCode.EXCEPTION));
			registrationStatusDto.setStatusComment(trimExceptionMessage
					.trimExceptionMessage(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getMessage() + e.getMessage()));
			registrationStatusDto.setSubStatusCode(StatusUtil.UNKNOWN_EXCEPTION_OCCURED.getCode());

			description.setMessage(PlatformErrorMessages.UNKNOWN_EXCEPTION.getMessage());
			description.setCode(PlatformErrorMessages.UNKNOWN_EXCEPTION.getCode());
			regProcLogger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
					regId, e.getMessage() + ExceptionUtils.getStackTrace(e));
		} finally {


			String moduleId = isTransactionSuccessful
					? PlatformSuccessMessages.RPR_DATA_VERIFICATION_APPROVED.getCode()
					: description.getCode();
			String moduleName = ModuleName.DATA_VERIFICATION.toString();
			registrationStatusService.updateRegistrationStatus(registrationStatusDto, moduleId, moduleName);

		}
		regProcLogger.debug(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.REGISTRATIONID.toString(),
				regId, "DataVerificationServiceImpl::updatePacketStatus()::exit");
		return isTransactionSuccessful;
	}

	private String validateRequestIdAndReturnRid(String reqId) throws Exception {
		List<DataVerificationEntity> record = dataVerificationRepository.findAllByRegId(reqId);

		if (CollectionUtils.isEmpty(record) || new HashSet<>(record).size() != 1) {
			regProcLogger.error("RID not found against request id : " + reqId);
			return null;
		}

		return record.get(0).getRegId();
	}
	private String saveDataVerificationData(String regId, String requestId){

		DataVerificationEntity dve = new DataVerificationEntity();
		dve.setRequestId(requestId);
		dve.setRegId(regId);
		dve.setDvUsrId(null);
		dve.setReasonCode("To be reviewed");
		dve.setStatusCode(DataVerificationStatus.PENDING.name());
		dve.setStatusComment("Assigned to verifiers");
		dve.setIsActive(true);
		dve.setIsDeleted(false);

		dve.setCrBy("SYSTEM");
		dve.setCrDtimes(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))));
		dve.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("UTC"))));
		dataVerificationRepository.save(dve);

		return "";
	}

	private boolean successFlow(String regId, DataVerificationResponseDTO dataVerificationDTO,
								DataVerificationEntity entity,
								InternalRegistrationStatusDto registrationStatusDto, MessageDTO messageDTO,
								LogDescription description) throws com.fasterxml.jackson.core.JsonProcessingException {

		boolean isTransactionSuccessful = false;
		String statusCode = dataVerificationDTO.getDecision().equalsIgnoreCase("APPROVED") ?
				DataVerificationStatus.APPROVED.name() : DataVerificationStatus.REJECTED.name();

		entity.setStatusCode(statusCode);
		entity.setStatusComment(statusCode.equalsIgnoreCase(DataVerificationStatus.APPROVED.name()) ?
				StatusUtil.DATA_VERIFIER_APPROVED_PACKET.getMessage() :
				StatusUtil.DATA_VERIFIER_REJECTED_PACKET.getMessage());
		entity.setReasonCode(dataVerificationDTO.getReason());
		isTransactionSuccessful = true;
		registrationStatusDto
				.setLatestTransactionTypeCode(RegistrationTransactionTypeCode.DATA_VERIFICATION.toString());
		registrationStatusDto.setRegistrationStageName(registrationStatusDto.getRegistrationStageName());


		if (statusCode != null && statusCode.equalsIgnoreCase(DataVerificationStatus.APPROVED.name())) {
			if (registrationStatusDto.getRegistrationType().equalsIgnoreCase(RegistrationType.LOST.toString())) {
					// TO DO
			}
			messageDTO.setIsValid(isTransactionSuccessful);
			dataVerificationStage.sendMessage(messageDTO);
			registrationStatusDto.setStatusComment(StatusUtil.DATA_VERIFIER_APPROVED_PACKET.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.DATA_VERIFIER_APPROVED_PACKET.getCode());
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.SUCCESS.toString());

			description.setMessage(PlatformSuccessMessages.RPR_DATA_VERIFICATION_APPROVED.getMessage());
			description.setCode(PlatformSuccessMessages.RPR_DATA_VERIFICATION_APPROVED.getCode());

		} else if (statusCode != null && statusCode.equalsIgnoreCase(DataVerificationStatus.REJECTED.name())) {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.REJECTED.toString());
			registrationStatusDto.setStatusComment(StatusUtil.DATA_VERIFIER_REJECTED_PACKET.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.DATA_VERIFIER_REJECTED_PACKET.getCode());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.FAILED.toString());

			description.setMessage(PlatformErrorMessages.RPR_DATA_VERIFICATION_REJECTED.getMessage());
			description.setCode(PlatformErrorMessages.RPR_DATA_VERIFICATION_REJECTED.getCode());
			messageDTO.setIsValid(Boolean.FALSE);
			dataVerificationStage.sendMessage(messageDTO);
		} else {
			registrationStatusDto.setStatusCode(RegistrationStatusCode.PROCESSING.toString());
			registrationStatusDto.setStatusComment(StatusUtil.RPR_DATA_VERIFICATION_RESEND.getMessage());
			registrationStatusDto.setSubStatusCode(StatusUtil.RPR_DATA_VERIFICATION_RESEND.getCode());
			registrationStatusDto
					.setLatestTransactionStatusCode(RegistrationTransactionStatusCode.IN_PROGRESS.toString());

			description.setMessage(PlatformErrorMessages.RPR_DATA_VERIFICATION_RESEND.getMessage());
			description.setCode(PlatformErrorMessages.RPR_DATA_VERIFICATION_RESEND.getCode());
			messageDTO.setIsValid(Boolean.FALSE);
			dataVerificationStage.sendMessage(messageDTO);
		}
		dataVerificationRepository.save(entity);
		return isTransactionSuccessful;
	}
}
