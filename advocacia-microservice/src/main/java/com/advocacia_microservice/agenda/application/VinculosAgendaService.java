package com.advocacia_microservice.agenda.application;
import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.domain.repository.VinculosAgendaRepository;
import com.advocacia_microservice.equipe.application.EquipeService;
import com.advocacia_microservice.empresa.domain.PapelEmpresa;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class VinculosAgendaService {
 private final VinculosAgendaRepository repository; private final EquipeService equipe;
 public VinculosAgendaService(VinculosAgendaRepository repository,EquipeService equipe){this.repository=repository;this.equipe=equipe;}
 private AcessoVinculos acesso(UUID usuario){
  var a=equipe.acesso(usuario);
  if(a.empresaId()==null || !a.modulos().contains("agenda") || !equipe.ativo(usuario))throw new AccessDeniedException("Você não tem acesso à Agenda.");
  return new AcessoVinculos(a.empresaId(),usuario,a.modulos().contains("crm"),a.modulos().contains("casos"),a.papel()==PapelEmpresa.MASTER);
 }
 public PaginaVinculos listar(UUID usuario,FiltroAgenda filtro,int pagina,int tamanho){
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100)throw new IllegalArgumentException("Paginação inválida.");
  return repository.listar(acesso(usuario),filtro,pagina,tamanho);
 }
 public List<MarcoAgenda> marcos(UUID usuario,FiltroAgenda filtro){return repository.marcos(acesso(usuario),filtro);}
}
