package it.polito.wa2.g15.validatorservice

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import it.polito.wa2.g15.validatorservice.dtos.FilterDto
import it.polito.wa2.g15.validatorservice.dtos.StatisticDto
import it.polito.wa2.g15.validatorservice.services.ValidationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import javax.imageio.ImageIO
import javax.validation.Valid

@RestController
class Controller {

    @Autowired
    lateinit var validationService: ValidationService

    @GetMapping("get/stats")
    fun getValidatorStats(
        @RequestParam(name = "timeStart", required = false) timeStart: String?,
        @RequestParam(name = "timeEnd", required = false) timeEnd: String?,
        @RequestParam(name = "nickname", required = false) nickname: String?
    ): ResponseEntity<StatisticDto> {
        val timeStartLocalDateTime = timeStart?.let { LocalDateTime.parse(it) }
        val timeEndLocalDateTime = timeEnd?.let { LocalDateTime.parse(it) }
        val res = validationService.getStats(timeStartLocalDateTime, timeEndLocalDateTime, nickname)
        return ResponseEntity<StatisticDto>(StatisticDto(validations = res), HttpStatus.ACCEPTED)
    }

    /**
     * It validates the ticket
     * @param clientZid is the zone of the turnstile
     * @param ticketByteArray ticket qr byte array
     */
    @PutMapping("/{clientZid}/validate")
    fun validateTicket(
        @PathVariable("clientZid") clientZid: String,
        @Valid @RequestBody ticketByteArray: ByteArray
    ): ResponseEntity<Boolean> {
        return try {
            val ticketQRis = ByteArrayInputStream(ticketByteArray)
            val ticketBI: BufferedImage = ImageIO.read(ticketQRis)
            val signedJwt: String = decodeQR(ticketBI)
            validationService.validateTicket(signedJwt, clientZid)
            ResponseEntity(HttpStatus.ACCEPTED)
        } catch (ex: Exception) {
            ResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        }
    }

    @Throws(IOException::class, NotFoundException::class)
    fun decodeQR(ticketBI: BufferedImage): String {
        val source: LuminanceSource = BufferedImageLuminanceSource(ticketBI)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result: Result = QRCodeReader().decode(bitmap)
        return result.text
    }


}