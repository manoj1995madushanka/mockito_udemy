package com.mockitotutorial.happyhotel.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BookingServiceTest {

    private BookingService bookingService;
    private PaymentService paymentService;
    private RoomService roomService;
    private BookingDAO bookingDAO;
    private MailSender mailSender;

    @BeforeEach
    void setup() {
        this.paymentService = mock(PaymentService.class);
        this.roomService = mock(RoomService.class);
        this.bookingDAO = mock(BookingDAO.class);
        this.mailSender = mock(MailSender.class);

        this.bookingService = new BookingService(paymentService, roomService, bookingDAO, mailSender);
    }

    @Test
    void should_calculate_correct_prices_for_correct_input()
    {
        //given
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022,01,01),
                LocalDate.of(2022,01,02),2,false);
        double expected = 2*2*50.0;

        // when
        double actual = bookingService.calculatePrice(bookingRequest);

        //then
        assertEquals(expected,actual);
    }

}