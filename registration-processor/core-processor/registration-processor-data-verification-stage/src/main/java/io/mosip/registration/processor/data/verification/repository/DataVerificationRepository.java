package io.mosip.registration.processor.data.verification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;

import io.mosip.registration.processor.packet.storage.entity.BasePacketEntity;
import io.mosip.registration.processor.data.verification.entity.DataVerificationEntity;

@Repository
public interface DataVerificationRepository extends JpaRepository<DataVerificationEntity,Long> {

    @Query(value = "SELECT dve FROM DataVerificationEntity dve WHERE dve.crDtimes in "
            + "(SELECT min(dve2.crDtimes) FROM DataVerificationEntity dve2 where dve2.statusCode=:statusCode) and dve.statusCode=:statusCode")
    public List<DataVerificationEntity> getFirstApplicantDetailsForAll(@Param("statusCode") String statusCode);

    @Query("SELECT dve FROM DataVerificationEntity dve where dve.regId=:regId and dve.dvUsrId=:dvUserId and dve.statusCode=:statusCode")
    public List<DataVerificationEntity> getSingleAssignedRecord(@Param("regId") String regId,
                                           @Param("dvUserId") String dvUserId, @Param("statusCode") String statusCode);

    @Query("SELECT dve FROM DataVerificationEntity dve where dve.id.regId=:regId and  dve.statusCode=:statusCode")
    public List<DataVerificationEntity> getAllAssignedRecord(@Param("regId") String regId, @Param("statusCode") String statusCode);

     @Query("SELECT dve FROM DataVerificationEntity dve where dve.dvUsrId=:dvUserId and dve.statusCode=:statusCode")
    public List<DataVerificationEntity> getAssignedApplicantDetails(@Param("dvUserId") String dvUserId,
                                               @Param("statusCode") String statusCode);

    @Query("SELECT dve FROM DataVerificationEntity dve where dve.regId=:regId")
    public DataVerificationEntity getDataVerificationEntityForRID(@Param("regId") String regId);

    List<DataVerificationEntity> findAllByRegId(String registrationId);
}
