import { Component, DestroyRef, PLATFORM_ID, inject, output, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, map, merge, of, switchMap, timer } from 'rxjs';
import { LembretesAgendaApiService } from '../services/lembretes-agenda-api.service';

@Component({
  selector:'app-agenda-contagem',
  template: '@if(total();as quantidade){<span aria-hidden="true">{{ quantidade > 99 ? "99+" : quantidade }}</span>}',
  styles: ':host{position:absolute;right:-4px;top:-5px;pointer-events:none}span{display:block;border-radius:1rem;background:#963e2c;color:white;font-size:10px;line-height:18px;min-width:18px;padding:0 3px;text-align:center}'
})
export class AgendaContagem {
  readonly contagem=output<number|null>();
  protected readonly total=signal<number|null>(null);
  private readonly api=inject(LembretesAgendaApiService);
  private readonly destroy=inject(DestroyRef);
  constructor(){
    if(!isPlatformBrowser(inject(PLATFORM_ID)))return;
    merge(timer(0,45000),this.api.alteracoes).pipe(
      switchMap(()=>this.api.listar(0,1).pipe(map(p=>p.total),catchError(()=>of(null)))),
      takeUntilDestroyed(this.destroy)
    ).subscribe(total=>{this.total.set(total);this.contagem.emit(total);});
  }
}
