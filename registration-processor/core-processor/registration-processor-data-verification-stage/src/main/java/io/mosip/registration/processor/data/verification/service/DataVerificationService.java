package io.mosip.registration.processor.data.verification.service;

import java.io.IOException;

import io.mosip.registration.processor.core.queue.factory.MosipQueue;
import io.mosip.registration.processor.data.verification.dto.DataVerificationResponseDTO;
import org.springframework.stereotype.Service;

import io.mosip.kernel.core.util.exception.JsonProcessingException;
import io.mosip.registration.processor.core.abstractverticle.MessageDTO;


@Service
public interface DataVerificationService {


	public boolean updatePacketStatus(DataVerificationResponseDTO resp, String stageName, MosipQueue queue);
	
	//public  void saveToDB(ManualAdjudicationResponseDTO res);

	public MessageDTO process(MessageDTO object, MosipQueue queue);

}
