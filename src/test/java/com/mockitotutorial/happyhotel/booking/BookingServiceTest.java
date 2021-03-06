package com.mockitotutorial.happyhotel.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

// this annotation needs to initialize annotated mocks
@ExtendWith(MockitoExtension.class)
// junit 5 uses extendWith and junit 4 uses runWith
class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private RoomService roomService;
    @Spy
    private BookingDAO bookingDAO;
    @Mock
    private MailSender mailSender;

    // argument captors
    private ArgumentCaptor<Double> doubleCaptor;
    private ArgumentCaptor<BookingRequest> bookingRequestCaptor;

    @BeforeEach
    void setup() {
        // below code was replaced with annotations
        /*this.paymentService = mock(PaymentService.class);
        this.roomService = mock(RoomService.class);
        this.bookingDAO = spy(BookingDAO.class);
        this.mailSender = mock(MailSender.class);*/

        this.bookingService = new BookingService(paymentService, roomService, bookingDAO, mailSender);

        System.out.println("List returned " + roomService.getAvailableRooms());
        System.out.println("Object returned " + roomService.findAvailableRoomId(null));
        System.out.println("Primitive returned " + roomService.getRoomCount());

        this.doubleCaptor = ArgumentCaptor.forClass(Double.class);
    }

    @Test
    void should_calculate_correct_prices_for_correct_input() {
        //given
        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2022, 01, 01),
                LocalDate.of(2022, 01, 02), 2, false);
        double expected = 2 * 2 * 50.0;

        // when
        double actual = bookingService.calculatePrice(bookingRequest);

        //then
        assertEquals(expected, actual);
    }

    @Test
    void should_countAvailable_places() {
        int expected = 0;
        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }

    @Test
    void should_countAvailablePlaces_when_oneRoomAvailable() {
        when(this.roomService.getAvailableRooms())
                .thenReturn(Collections.singletonList(new Room("Room 1", 2)));

        int expected = 2;
        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }

    @Test
    void should_countAvailablePlaces_when_multipleRoomAvailable() {
        List<Room> rooms = Arrays.asList(new Room("Room 1", 2), new Room("Room 2", 5));
        when(this.roomService.getAvailableRooms())
                .thenReturn(rooms);

        int expected = 7;
        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }

    @Test
    void should_countAvailablePlaces_when_calledMultipleTimes() {
        when(this.roomService.getAvailableRooms())
                .thenReturn(Collections.singletonList(new Room("Room 1", 2)))
                .thenReturn(Collections.emptyList());

        int expectedFirstCall = 2;
        int expectedSecondCall = 0;
        int actualFirstCall = bookingService.getAvailablePlaceCount();
        int actualSecondCall = bookingService.getAvailablePlaceCount();

        assertAll(
                () -> assertEquals(expectedFirstCall, actualFirstCall),
                () -> assertEquals(expectedSecondCall, actualSecondCall)
        );
    }

    @Test
    void should_throw_exception_when_no_room_available() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, false);
        when(roomService.findAvailableRoomId(bookingRequest)).thenThrow(BusinessException.class);

        Executable executable = () -> bookingService.makeBooking(bookingRequest);

        assertThrows(BusinessException.class, executable);
    }


    @Test
    void should_not_complete_booking_when_price_too_high() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, true);
        when(paymentService.pay(any(), anyDouble())).thenThrow(BusinessException.class);

        Executable executable = () -> bookingService.makeBooking(bookingRequest);

        assertThrows(BusinessException.class, executable);
    }

    @Test
    void should_invoke_payment_when_prepaid() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, true);

        bookingService.makeBooking(bookingRequest);

        // verify that method is called
        verify(paymentService, times(1)).pay(bookingRequest, 400.0);
        // check if any other calls goes to payment service
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void should_not_invoke_payment_when_not_prepaid() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, false);

        bookingService.makeBooking(bookingRequest);

        verify(paymentService, never()).pay(any(), anyDouble());
    }

    @Test
    void should_make_booking_when_input_ok() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, true);

        String bookingId = bookingService.makeBooking(bookingRequest);

        verify(bookingDAO).save(bookingRequest);
        // when we use mock bookingService booking id will be null
        // when we use spy bookingService booking id will be random value
        // mock = dummy object with no real values
        // spy = real object with real logic that we can modify
        System.out.println("booking Id = " + bookingId);
    }

    @Test
    void should_cancel_booking_when_input_ok() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, true);
        bookingRequest.setRoomId("1.3");
        String bookingId = "1";

        // mocks : when(mock.method()).thenReturn()
        // spies : doReturn().when(spy).method()

        doReturn(bookingRequest).when(bookingDAO).get(bookingId);


        bookingService.cancelBooking(bookingId);
    }


    //mock void method
    @Test
    void should_throw_exception_when_no_room_available_mock_void() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, false);

        // this is void method
        doThrow(BusinessException.class).when(mailSender).sendBookingConfirmation(any());

        Executable executable = () -> bookingService.makeBooking(bookingRequest);

        assertThrows(BusinessException.class, executable);
    }

    @Test
    void should_not_throw_exception_when_mail_not_ready() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, false);

        // this is void method
        doNothing().when(mailSender).sendBookingConfirmation(any());

        bookingService.makeBooking(bookingRequest);
    }

    @Test
    void should_correct_price_when_input_ok() {
        BookingRequest bookingRequest = new BookingRequest("1",
                LocalDate.of(2020, 05, 01), LocalDate.of(2020, 05, 03),
                2, true);

        bookingService.makeBooking(bookingRequest);

        verify(paymentService, times(1)).pay(eq(bookingRequest), doubleCaptor.capture());
        double capturedArgument = doubleCaptor.getValue();

        assertEquals(400.0, capturedArgument);

    }

    @Test
    void should_correct_price_when_multiple_input() {
        BookingRequest bookingRequest1 = new BookingRequest("1",
                LocalDate.of(2020, 05, 01),
                LocalDate.of(2020, 05, 03),
                2, true);

        BookingRequest bookingRequest2 = new BookingRequest("1",
                LocalDate.of(2020, 05, 01),
                LocalDate.of(2020, 05, 02),
                2, true);

        List<Double> expectedValues = Arrays.asList(400.0, 100.0);

        bookingService.makeBooking(bookingRequest1);
        bookingService.makeBooking(bookingRequest2);

        verify(paymentService, times(2)).pay(any(), doubleCaptor.capture());
        List<Double> capturedArguments = doubleCaptor.getAllValues();

        assertEquals(expectedValues, capturedArguments);

    }


}