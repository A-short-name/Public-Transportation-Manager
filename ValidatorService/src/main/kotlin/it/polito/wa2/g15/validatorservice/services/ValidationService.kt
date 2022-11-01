package it.polito.wa2.g15.validatorservice.services

import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.dtos.StatisticDto
import org.springframework.stereotype.Service

@Service
class ValidationService {

    /**
     * returns statistics of this validator
     *
     * @param filter the filters are applied only if the field of the filter object aren't null,
     * otherwise that specific filter will be ignored
     */
    fun getStats(filter: FilterDto) : StatisticDto{
        TODO("Not yet implemented")
    }
}
