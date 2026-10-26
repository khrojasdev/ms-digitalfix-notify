package cl.duoc.digitalfix.notify.web;

import cl.duoc.digitalfix.notify.service.TicketDespachoService;
import cl.duoc.digitalfix.notify.web.dto.TicketDespachoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tickets de despacho emitidos a los tecnicos. Llega por el BFF
 * (GET /api/avisos/**), que agrega X-Company-Id: cada empresa ve solo los suyos.
 */
@RestController
@RequestMapping("/api/avisos/despachos")
public class DespachoController {

    private final TicketDespachoService tickets;

    public DespachoController(TicketDespachoService tickets) {
        this.tickets = tickets;
    }

    @GetMapping
    public List<TicketDespachoDto> recientes(@RequestHeader(value = "X-Company-Id", required = false) Long empresaId) {
        return tickets.recientes(empresaId);
    }
}
